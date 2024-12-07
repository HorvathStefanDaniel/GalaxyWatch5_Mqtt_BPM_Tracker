package com.example.measuredata

import android.content.Context
import android.util.Log
import androidx.concurrent.futures.await
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.*
import androidx.health.services.client.unregisterMeasureCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

class HealthServicesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    healthServicesClient: HealthServicesClient
) {
    private val measureClient = healthServicesClient.measureClient

    companion object {
        private const val TAG = "HealthServicesManager"
    }

    // SharedFlow to emit MeasureMessages to collectors
    private val _measureFlow = MutableSharedFlow<MeasureMessage>(replay = 0)
    val measureFlow: SharedFlow<MeasureMessage> = _measureFlow

    // Single callback instance
    private val callback = object : MeasureCallback {
        override fun onAvailabilityChanged(
            dataType: DeltaDataType<*, *>,
            availability: Availability
        ) {
            if (availability is DataTypeAvailability) {
                Log.d(TAG, "Availability changed: $availability")
                // Emit availability changes asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    _measureFlow.emit(MeasureMessage.MeasureAvailability(availability))
                }
            }
        }

        override fun onDataReceived(data: DataPointContainer) {
            // Collect heart rate BPM data
            val heartRateBpm: List<SampleDataPoint<Double>> =
                data.getData(DataType.HEART_RATE_BPM)
            if (heartRateBpm.isNotEmpty()) {
                val latestDataPoint = heartRateBpm.last()
                val bpm = latestDataPoint.value
                val timestamp = latestDataPoint.timeDurationFromBoot.toMillis()

                Log.d(TAG, "Heart rate data received: $bpm bpm at $timestamp ms")

                // Always emit MeasureData, including when bpm <= 0
                CoroutineScope(Dispatchers.IO).launch {
                    _measureFlow.emit(MeasureMessage.MeasureData(listOf(latestDataPoint)))
                }

                if (bpm > 0) { // Only estimate IBI if BPM is valid
                    // Estimate IBI based on BPM
                    val ibi = (60000.0 / bpm).toLong()

                    Log.d(TAG, "Estimated IBI: $ibi ms based on BPM")

                    // Emit IBI data asynchronously
                    CoroutineScope(Dispatchers.IO).launch {
                        _measureFlow.emit(MeasureMessage.MeasureIBI(ibi))
                    }
                } else {
                    Log.w(TAG, "Received invalid BPM value: $bpm. Handling in ViewModel.")
                }
            } else {
                Log.d(TAG, "No heart rate data received.")
            }
        }
    }

    init {
        // Register the measure callback for heart rate BPM data type
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Registering for heart rate data")
                measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register measure callback: ${e.message}")
            }
        }

        // Optionally, handle unregistration when the manager is destroyed
    }

    /**
     * Checks if the device has heart rate capability.
     */
    suspend fun hasHeartRateCapability(): Boolean {
        return try {
            val capabilities = measureClient.getCapabilitiesAsync().await()
            Log.d(TAG, "Supported data types: ${capabilities.supportedDataTypesMeasure}")
            DataType.HEART_RATE_BPM in capabilities.supportedDataTypesMeasure
        } catch (e: Exception) {
            Log.e(TAG, "Error checking heart rate capability: ${e.message}")
            false
        }
    }

    /**
     * Unregisters the measure callback. Call this when the manager is no longer needed.
     */
    fun unregister() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Unregistering for heart rate data")
                measureClient.unregisterMeasureCallback(DataType.HEART_RATE_BPM, callback)
                Log.d(TAG, "Successfully unregistered measure callback")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering measure callback: ${e.message}")
            }
        }
    }
}

// Define the MeasureMessage sealed class if not already defined
sealed class MeasureMessage {
    data class MeasureAvailability(val availability: DataTypeAvailability) : MeasureMessage()
    data class MeasureData(val data: List<SampleDataPoint<Double>>) : MeasureMessage()
    data class MeasureIBI(val ibi: Long) : MeasureMessage()
}
