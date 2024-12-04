package com.example.measuredata

import android.content.Context
import android.util.Log
import androidx.concurrent.futures.await
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.*
import androidx.health.services.client.unregisterMeasureCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class HealthServicesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    healthServicesClient: HealthServicesClient
) {
    private val measureClient = healthServicesClient.measureClient

    companion object {
        private const val TAG = "HealthServicesManager"
    }

    suspend fun hasHeartRateCapability(): Boolean {
        val capabilities = measureClient.getCapabilitiesAsync().await()
        Log.d(TAG, "Supported data types: ${capabilities.supportedDataTypesMeasure}")

        return (DataType.HEART_RATE_BPM in capabilities.supportedDataTypesMeasure)
    }
    @ExperimentalCoroutinesApi
    fun heartRateMeasureFlow() = callbackFlow {
        val callback = object : MeasureCallback {
            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>,
                availability: Availability
            ) {
                if (availability is DataTypeAvailability) {
                    Log.d(TAG, "Availability changed: $availability")
                    trySend(MeasureMessage.MeasureAvailability(availability)).isSuccess
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

                    // Estimate IBI based on BPM
                    var ibi = 0.toLong()
                    ibi = if(bpm != 0.toDouble()){
                        (60000.0 / bpm).toLong()
                    }else{
                        0
                    }

                    Log.d(TAG, "Estimated IBI: $ibi ms based on BPM")

                    // Send IBI data
                    trySend(MeasureMessage.MeasureIBI(ibi)).isSuccess

                    // Send heart rate data
                    trySend(MeasureMessage.MeasureData(listOf(latestDataPoint))).isSuccess
                } else {
                    Log.d(TAG, "No heart rate data received.")
                }
            }
        }

        Log.d(TAG, "Registering for heart rate data")
        // Register the measure callback for heart rate BPM data type
        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)

        awaitClose {
            Log.d(TAG, "Unregistering for heart rate data")
            // Unregister the measure callback
            runBlocking {
                measureClient.unregisterMeasureCallback(DataType.HEART_RATE_BPM, callback)
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
