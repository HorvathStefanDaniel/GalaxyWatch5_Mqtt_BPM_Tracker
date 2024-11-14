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
    private var onlineMode = true

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
        var serverUri = sharedPreferences.getString("serverUri", "127.0.0.1") ?: "127.0.0.1"
        val port = sharedPreferences.getInt("port", 1883)

        // Ensure that the server URI has the correct format (add "tcp://" if it's missing)
        if (!serverUri.contains("://")) {
            serverUri = "tcp://$serverUri"
        }

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
            var retryCount = 0
            val maxRetries = 1
            while (retryCount < maxRetries) {
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
                    }
                    break // Exit loop after success

                } catch (e: MqttException) {
                    retryCount++
                    Log.e(TAG, "Failed to connect to broker (attempt $retryCount): ${e.message}")

                    if (retryCount < maxRetries) {
                        // Update UI to reflect that we're retrying
                        withContext(Dispatchers.Main) {
                            _uiState.value = UiState.ConnectingToMQTT // Still trying
                            Log.d(TAG, "Retrying MQTT connection... (attempt $retryCount)")
                        }
                    } else {
                        // Max retries reached, update UI to reflect failure
                        Log.e(TAG, "Max retries reached. Unable to connect to MQTT broker.")
                        withContext(Dispatchers.Main) {
                            _uiState.value = UiState.FailedToConnectMQTT
                        }
                    }
                }
            }
        }
    }

    // Update MQTT details from the UI and save them in SharedPreferences
    fun updateMqttDetails(serverUri: String, username: String?, password: String?, port: Int) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating MQTT details - Server URI: $serverUri, Port: $port, Username: $username, Password: ${if (password != null) "****" else "null"}")

                mqttClient.disconnect()
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

            } catch (e: MqttException) {
                Log.e(TAG, "Failed to update MQTT details: ${e.message}")
            }
        }
    }

    // Publish heart rate data to MQTT
    private fun publishHeartRateData(bpm: Double) {
        val message = MqttMessage("Heart Rate: $bpm".toByteArray())
        try {
            if (::mqttClient.isInitialized && mqttClient.isConnected) {
                mqttClient.publish("watch/heart_rate", message)  // Updated topic to reflect heart rate
                Log.d(TAG, "Published message: Heart Rate: $bpm")
            } else {
                Log.e(TAG, "MQTT client is not connected, failed to publish message")
            }
        } catch (e: MqttException) {
            Log.e(TAG, "Failed to publish message: ${e.message}")
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
                } catch (e: MqttException) {
                    Log.e(TAG, "Failed to disconnect MQTT: ${e.message}")
                    onlineMode = false
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun measureHeartRate() {
        viewModelScope.launch {
            Log.d(TAG, "Starting to measure heart rate.")
            healthServicesManager.heartRateMeasureFlow().collect {
                when (it) {
                    is MeasureMessage.MeasureAvailability -> {
                        Log.d(TAG, "Availability changed: ${it.availability}")
                        _heartRateAvailable.value = it.availability
                    }
                    is MeasureMessage.MeasureData -> {
                        if (it.data.isNotEmpty()) {
                            val bpm = it.data.last().value
                            Log.d(TAG, "Data update: $bpm")
                            _heartRateBpm.value = bpm
                            if (onlineMode) {
                                publishHeartRateData(bpm)  // Publishing heart rate data
                            }
                        } else {
                            Log.d(TAG, "Received empty heart rate data.")
                        }
                    }
                }
            }
        }
    }
}

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
    object ConnectingToMQTT : UiState()  // New state for MQTT connection progress
    object ConnectedToMQTT : UiState()   // New state for when MQTT is connected
    object FailedToConnectMQTT : UiState() // New state for connection failure
}

