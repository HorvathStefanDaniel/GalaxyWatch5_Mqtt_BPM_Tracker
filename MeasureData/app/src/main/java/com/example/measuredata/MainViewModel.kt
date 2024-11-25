package com.example.measuredata

import android.content.SharedPreferences
import android.util.Log
import androidx.health.services.client.data.DataTypeAvailability
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val healthServicesManager: HealthServicesManager,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Startup)
    val uiState: StateFlow<UiState> = _uiState

    private val _heartRateAvailable = MutableStateFlow(DataTypeAvailability.UNKNOWN)
    val heartRateAvailable: StateFlow<DataTypeAvailability> = _heartRateAvailable

    private val _heartRateBpm = MutableStateFlow(0.0)
    val heartRateBpm: StateFlow<Double> = _heartRateBpm

    private lateinit var mqttClient: MqttClient
    private var onlineMode = false

    // Overwrite BPM functionality
    val overwriteBpmEnabled = MutableStateFlow(false)
    val overwriteBpmValue = MutableStateFlow(0.0)

    private val _ibiValue = MutableStateFlow<Long?>(null)
    val ibiValue: StateFlow<Long?> = _ibiValue

    // Variables to store latest BPM and IBI values
    private var latestBpm: Double? = null
    private var latestIbi: Long? = null

    init {
        // Setup MQTT with stored preferences initially
        setupMqtt()

        // Check heart rate capability
        viewModelScope.launch {
            _uiState.value = if (healthServicesManager.hasHeartRateCapability()) {
                UiState.HeartRateAvailable
            } else {
                UiState.HeartRateNotAvailable
            }
        }
    }

    private fun getStoredMqttDetails(): MqttDetails {
        val serverUri = sharedPreferences.getString("serverUri", "tcp://127.0.0.1") ?: "tcp://127.0.0.1"
        val port = sharedPreferences.getInt("port", 1883)
        val username = sharedPreferences.getString("username", null)
        val password = sharedPreferences.getString("password", null)

        // Log the retrieved MQTT details
        Log.d(TAG, "MQTT Details - Server URI: $serverUri, Port: $port, Username: $username, Password: ${if (password != null) "****" else "null"}")

        return MqttDetails(serverUri, port, username, password)
    }

    fun setupMqtt() {
        val mqttDetails = getStoredMqttDetails()

        // Set the UI state to show that we're connecting
        _uiState.value = UiState.ConnectingToMQTT

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to connect to MQTT broker at ${mqttDetails.serverUri}:${mqttDetails.port}")
                mqttClient = MqttClient("${mqttDetails.serverUri}:${mqttDetails.port}", MqttClient.generateClientId(), MemoryPersistence())
                val connOpts = MqttConnectOptions().apply {
                    isCleanSession = true
                    mqttDetails.username?.let {
                        Log.d(TAG, "Using MQTT username: $it")
                        userName = it
                    }
                    mqttDetails.password?.let {
                        Log.d(TAG, "Using MQTT password: ****")
                        setPassword(it.toCharArray())
                    }
                }

                mqttClient.connect(connOpts)

                // Connection successful, update UI state
                Log.d(TAG, "Connected to broker at ${mqttDetails.serverUri}:${mqttDetails.port}")
                withContext(Dispatchers.Main) {
                    _uiState.value = UiState.ConnectedToMQTT
                    onlineMode = true
                }

            } catch (e: MqttException) {
                Log.e(TAG, "Failed to connect to broker: ${e.message}")
                withContext(Dispatchers.Main) {
                    _uiState.value = UiState.FailedToConnectMQTT
                    onlineMode = false
                }
            }
        }
    }

    // Update MQTT details from the UI and save them in SharedPreferences
    fun updateMqttDetails(serverUri: String, username: String?, password: String?, port: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating MQTT details - Server URI: $serverUri, Port: $port, Username: $username, Password: ${if (password != null) "****" else "null"}")

                if (::mqttClient.isInitialized && mqttClient.isConnected) {
                    mqttClient.disconnect()
                }

                mqttClient = MqttClient("tcp://$serverUri:$port", MqttClient.generateClientId(), MemoryPersistence())
                val connOpts = MqttConnectOptions().apply {
                    isCleanSession = true
                    // Set username and password if provided
                    username?.let {
                        Log.d(TAG, "Using MQTT username: $it")
                        userName = it
                    }
                    password?.let {
                        Log.d(TAG, "Using MQTT password: ****")  // Don't log the actual password
                        setPassword(it.toCharArray())
                    }
                }

                mqttClient.connect(connOpts)
                Log.d(TAG, "MQTT details updated and connected to broker at $serverUri:$port")

                // Save the new settings to SharedPreferences
                sharedPreferences.edit()
                    .putString("serverUri", serverUri)
                    .putInt("port", port)
                    .putString("username", username)
                    .putString("password", password)
                    .apply()

                // Update UI state
                _uiState.value = UiState.ConnectedToMQTT
                onlineMode = true

            } catch (e: MqttException) {
                Log.e(TAG, "Failed to update MQTT details: ${e.message}")
                _uiState.value = UiState.FailedToConnectMQTT
                onlineMode = false
            }
        }
    }

    // Function to publish combined heart rate and IBI data
    private fun publishHeartData(bpm: Double, ibi: Long) {
        val heartData = HeartData(bpm, ibi)
        val messagePayload = "HeartData: bpm=${heartData.bpm}, ibi=${heartData.ibi}"
        val message = MqttMessage(messagePayload.toByteArray())
        try {
            if (::mqttClient.isInitialized && mqttClient.isConnected) {
                mqttClient.publish("watch/heart_data", message)
                Log.d(TAG, "Published combined heart data: $messagePayload")
            } else {
                Log.e(TAG, "MQTT client is not connected, failed to publish heart data")
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Failed to publish heart data: ${e.message}")
        }
    }

    // Stop the MQTT client
    fun stopMqtt() {
        if (::mqttClient.isInitialized && mqttClient.isConnected) {
            viewModelScope.launch {
                try {
                    mqttClient.disconnect()
                    Log.d(TAG, "MQTT disconnected")
                    onlineMode = false
                    _uiState.value = UiState.FailedToConnectMQTT
                } catch (e: MqttException) {
                    Log.e(TAG, "Failed to disconnect MQTT: ${e.message}")
                    onlineMode = false
                }
            }
        } else {
            onlineMode = false
            _uiState.value = UiState.FailedToConnectMQTT
        }
    }

    @ExperimentalCoroutinesApi
    fun measureHeartRate() {
        viewModelScope.launch {
            Log.d(TAG, "Starting to measure heart rate.")
            healthServicesManager.heartRateMeasureFlow().collect { measureMessage ->
                when (measureMessage) {
                    is MeasureMessage.MeasureAvailability -> {
                        Log.d(TAG, "Availability changed: ${measureMessage.availability}")
                        _heartRateAvailable.value = measureMessage.availability
                    }
                    is MeasureMessage.MeasureData -> {
                        if (measureMessage.data.isNotEmpty()) {
                            var bpm = measureMessage.data.last().value
                            Log.d(TAG, "Data update: $bpm")

                            // Check if overwrite is enabled
                            if (overwriteBpmEnabled.value) {
                                bpm = overwriteBpmValue.value
                                Log.d(TAG, "Overwrite enabled. Using BPM value: $bpm")
                            }

                            _heartRateBpm.value = bpm
                            latestBpm = bpm

                            // Try to publish combined data if both bpm and ibi are available
                            attemptToPublishCombinedData()
                        } else {
                            Log.d(TAG, "Received empty heart rate data.")
                        }
                    }
                    is MeasureMessage.MeasureIBI -> {
                        val ibi = measureMessage.ibi
                        Log.d(TAG, "Received IBI: $ibi ms")
                        _ibiValue.value = ibi
                        latestIbi = ibi

                        // Try to publish combined data if both bpm and ibi are available
                        attemptToPublishCombinedData()
                    }
                }
            }
        }
    }

    private fun attemptToPublishCombinedData() {
        if (latestBpm != null && latestIbi != null) {
            if (onlineMode) {
                publishHeartData(latestBpm!!, latestIbi!!)
                // Reset the latest values after publishing
                latestBpm = null
                latestIbi = null
            }
        }
    }

    // Functions to update overwrite BPM values
    fun setOverwriteBpmEnabled(enabled: Boolean) {
        overwriteBpmEnabled.value = enabled
    }

    fun setOverwriteBpmValue(value: Double) {
        overwriteBpmValue.value = value
    }
}

// Helper data class for combined heart data
data class HeartData(val bpm: Double, val ibi: Long)

// Helper data class for MQTT details
data class MqttDetails(
    val serverUri: String,
    val port: Int,
    val username: String?,
    val password: String?
)

sealed class UiState {
    object Startup : UiState()
    object HeartRateAvailable : UiState()
    object HeartRateNotAvailable : UiState()
    object ConnectingToMQTT : UiState()
    object ConnectedToMQTT : UiState()
    object FailedToConnectMQTT : UiState()
}
