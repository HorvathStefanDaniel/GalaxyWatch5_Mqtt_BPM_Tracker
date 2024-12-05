package com.example.measuredata

import android.util.Log
import androidx.health.services.client.data.DataTypeAvailability
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val serverIpAddress = "192.168.18.3"//"192.168.1.99" // Update with your server's IP address
    private val serverPort = 6009 // Port on which the server is listening for UDP packets

    init {
        // Check heart rate capability
        viewModelScope.launch {
            _uiState.value = if (healthServicesManager.hasHeartRateCapability()) {
                UiState.HeartRateAvailable
            } else {
                UiState.HeartRateNotAvailable
            }
        }
    }

    @ExperimentalCoroutinesApi
    fun measureHeartRate() {
        if (isMeasuring) {
            Log.d(TAG, "Stopping measurement.")
            measurementJob?.cancel()
            isMeasuring = false
            measurementJob = null
            return
        }
        isMeasuring = true
        measurementJob = viewModelScope.launch {
            Log.d(TAG, "Starting to measure heart rate.")
            healthServicesManager.heartRateMeasureFlow().collect { measureMessage ->
                when (measureMessage) {
                    is MeasureMessage.MeasureAvailability -> {
                        Log.d(TAG, "Availability changed: ${measureMessage.availability}")
                        _heartRateAvailable.value = measureMessage.availability
                    }
                    is MeasureMessage.MeasureData -> {
                        if (measureMessage.data.isNotEmpty()) {
                            val bpm = measureMessage.data.last().value
                            Log.d(TAG, "Data update: $bpm")

                            _heartRateBpm.value = bpm
                            latestBpm = bpm

                            // Try to send combined data if both bpm and ibi are available
                            attemptToSendCombinedData()
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

    private fun attemptToSendCombinedData() {
        // Send only when both BPM and IBI are available
        if (latestBpm != null && latestIbi != null) {
            sendHeartData(latestBpm!!, latestIbi!!)
            // Reset the latest values after sending
            latestBpm = null
            latestIbi = null
        }
    }

    // Function to send combined heart rate and IBI data via UDP
    private fun sendHeartData(bpm: Double, ibi: Long) {
        val heartData = HeartData(bpm, ibi)
        val messagePayload = "HeartData: bpm=${heartData.bpm}, ibi=${heartData.ibi}"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val socket = DatagramSocket()
                Log.e(TAG, "Socket: $socket is connected ? ${socket.isConnected}")
                val serverAddress = InetAddress.getByName(serverIpAddress)
                val buffer = messagePayload.toByteArray()
                val packet = DatagramPacket(buffer, buffer.size, serverAddress, serverPort)
                socket.send(packet)
                socket.close()
                Log.d(TAG, "Sent heart data via UDP: $messagePayload to ${serverIpAddress}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send heart data via UDP: ${e.message}")
            }
        }
    }

    // Helper data class for combined heart data
    data class HeartData(val bpm: Double, val ibi: Long)
}

sealed class UiState {
    object Startup : UiState()
    object HeartRateAvailable : UiState()
    object HeartRateNotAvailable : UiState()
}
