package com.example.measuredata

import android.util.Log
import androidx.health.services.client.data.DataTypeAvailability
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val healthServicesManager: HealthServicesManager
) : ViewModel() {

    companion object {
        private const val TAG = "MainViewModel"
        private const val RETRY_DELAY_MS = 2000L // Delay between retries in milliseconds
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Startup)
    val uiState: StateFlow<UiState> = _uiState

    private val _heartRateAvailable = MutableStateFlow(DataTypeAvailability.UNKNOWN)
    val heartRateAvailable: StateFlow<DataTypeAvailability> = _heartRateAvailable

    private val _heartRateBpm = MutableStateFlow(0.0)
    val heartRateBpm: StateFlow<Double> = _heartRateBpm

    private var isMeasuring = false // Flag to track measurement state
    private var measurementJob: Job? = null // Track the measurement coroutine job

    private val _ibiValue = MutableStateFlow<Long?>(null)
    val ibiValue: StateFlow<Long?> = _ibiValue

    // Variables to store latest BPM and IBI values
    private var latestBpm: Double? = null
    private var latestIbi: Long? = null  // Initialize as null

    // UDP Socket details
    private val serverIpAddress = "192.168.1.11"
    private val serverPort = 6009 // Port on which the server is listening for UDP packets

    // Flag to prevent multiple re-initializations
    private var isReinitializing = false

    init {
        // Automatically start measuring heart rate upon initialization
        viewModelScope.launch {
            if (healthServicesManager.hasHeartRateCapability()) {
                Log.d(TAG, "Heart rate capability available. Starting measurement.")
                _uiState.value = UiState.HeartRateAvailable
                startHeartRateMeasurement()
            } else {
                Log.e(TAG, "Heart rate capability not available.")
                _uiState.value = UiState.HeartRateNotAvailable
                // Optionally, implement a retry mechanism to check capability again after some time
            }
        }
    }

    @ExperimentalCoroutinesApi
    private fun startHeartRateMeasurement() {
        if (isMeasuring) {
            Log.d(TAG, "Measurement already in progress.")
            return
        }
        isMeasuring = true
        measurementJob = viewModelScope.launch {
            Log.d(TAG, "Starting to measure heart rate.")
            healthServicesManager.measureFlow.collect { measureMessage ->
                when (measureMessage) {
                    is MeasureMessage.MeasureAvailability -> {
                        Log.d(TAG, "Availability changed: ${measureMessage.availability}")
                        _heartRateAvailable.value = measureMessage.availability
                    }
                    is MeasureMessage.MeasureData -> {
                        if (measureMessage.data.isNotEmpty()) {
                            val bpm = measureMessage.data.last().value
                            Log.d(TAG, "Data update: $bpm")

                            if (bpm > 0) {
                                _heartRateBpm.value = bpm
                                latestBpm = bpm
                                // Reset re-initialization flag on valid reading
                                isReinitializing = false

                                // Try to send combined data if both bpm and ibi are available
                                attemptToSendCombinedData()
                            } else {
                                Log.w(TAG, "Received invalid BPM: $bpm. Attempting to re-initialize sensor.")
                                handleInvalidBpm()
                            }
                        } else {
                            Log.d(TAG, "Received empty heart rate data.")
                        }
                    }
                    is MeasureMessage.MeasureIBI -> {
                        val ibi = measureMessage.ibi
                        Log.d(TAG, "Received IBI: $ibi ms")
                        _ibiValue.value = ibi
                        latestIbi = ibi

                        // Try to send combined data if both bpm and ibi are available
                        attemptToSendCombinedData()
                    }
                }
            }
        }
    }

    /**
     * Attempts to send combined heart rate and IBI data via UDP.
     */
    private fun attemptToSendCombinedData() {
        // Send only when both BPM and IBI are available and BPM is valid
        if (latestBpm != null && latestIbi != null && latestBpm!! > 0) {
            sendHeartData(latestBpm!!, latestIbi!!)
            // Reset the latest values after sending
            latestBpm = null
            latestIbi = null
        }
    }

    /**
     * Handles scenarios where an invalid BPM reading (e.g., 0 bpm) is received.
     * Initiates a re-initialization of the heart rate sensor.
     */
    private fun handleInvalidBpm() {
        if (isReinitializing) {
            Log.d(TAG, "Re-initialization already in progress.")
            return
        }

        isReinitializing = true
        Log.d(TAG, "Attempting to re-initialize sensor after invalid BPM.")

        viewModelScope.launch {
            delay(RETRY_DELAY_MS)
            reinitializeHeartRateMeasurement()
        }
    }

    /**
     * Re-initializes the heart rate measurement by stopping and restarting the measurement job.
     */
    private suspend fun reinitializeHeartRateMeasurement() {
        Log.d(TAG, "Re-initializing heart rate measurement.")
        stopHeartRateMeasurement()
        startHeartRateMeasurement()
    }

    /**
     * Stops the current heart rate measurement coroutine job.
     */
    private fun stopHeartRateMeasurement() {
        if (isMeasuring) {
            measurementJob?.cancel()
            isMeasuring = false
            Log.d(TAG, "Stopped heart rate measurement.")
        }
    }

    /**
     * Function to send combined heart rate and IBI data via UDP.
     */
    private fun sendHeartData(bpm: Double, ibi: Long) {
        val heartData = HeartData(bpm, ibi)
        val messagePayload = "HeartData: bpm=${heartData.bpm}, ibi=${heartData.ibi}"

        // Return on invalid bpm data
        if (heartData.bpm <= 0.0) {
            Log.e(TAG, "Invalid BPM value: ${heartData.bpm}")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                DatagramSocket().use { socket ->
                    val serverAddress = InetAddress.getByName(serverIpAddress)
                    val buffer = messagePayload.toByteArray()
                    val packet = DatagramPacket(buffer, buffer.size, serverAddress, serverPort)
                    socket.send(packet)
                    Log.d(TAG, "Sent heart data via UDP: $messagePayload to $serverIpAddress")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send heart data via UDP: ${e.message}")
                // Optionally implement retry logic here if UDP transmission fails
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopHeartRateMeasurement()
        healthServicesManager.unregister()
    }

    // Helper data class for combined heart data
    data class HeartData(val bpm: Double, val ibi: Long)
}

sealed class UiState {
    object Startup : UiState()
    object HeartRateAvailable : UiState()
    object HeartRateNotAvailable : UiState()
}
