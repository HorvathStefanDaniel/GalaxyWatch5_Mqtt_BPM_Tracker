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

    // Variable to store the timestamp of the last heart rate data point
    private var lastHeartRateTimestamp: Long? = null

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

                    // Calculate IBI if last timestamp is available
                    if (lastHeartRateTimestamp != null) {
                        val ibi = timestamp - lastHeartRateTimestamp!!
                        Log.d(TAG, "Calculated IBI: $ibi ms")

                        // Send IBI data
                        trySend(MeasureMessage.MeasureIBI(ibi)).isSuccess
                    }

                    // Update last timestamp
                    lastHeartRateTimestamp = timestamp

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
            // Wrap the suspending function call inside runBlocking
            runBlocking {
                measureClient.unregisterMeasureCallback(DataType.HEART_RATE_BPM, callback)
            }
        }
    }
}

sealed class MeasureMessage {
    class MeasureAvailability(val availability: DataTypeAvailability) : MeasureMessage()
    class MeasureData(val data: List<SampleDataPoint<Double>>) : MeasureMessage()
    class MeasureIBI(val ibi: Long) : MeasureMessage()
}
