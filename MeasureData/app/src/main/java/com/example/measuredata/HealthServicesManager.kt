package com.example.measuredata

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.concurrent.futures.await
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import androidx.health.services.client.data.SampleDataPoint
import androidx.health.services.client.unregisterMeasureCallback
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class HealthServicesManager @Inject constructor(
    private val context: Context,
    healthServicesClient: HealthServicesClient
) : SensorEventListener {
    private val measureClient = healthServicesClient.measureClient

    companion object {
        private const val TAG = "HealthServicesManager"
    }

    // SensorManager and sensors
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var heartBeatSensor: Sensor? = null

    // Variables to store timestamps for IBI calculation
    private var lastBeatTimestamp: Long = 0L

    // Channel to communicate sensor data
    private val sensorChannel = Channel<MeasureMessage>(Channel.BUFFERED)

    // Function to check if the heart beat sensor is available
    fun isHeartBeatSensorAvailable(): Boolean {
        heartBeatSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_BEAT)
        return heartBeatSensor != null
    }

    suspend fun hasHeartRateCapability(): Boolean {
        val capabilities = measureClient.getCapabilitiesAsync().await()
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
                val heartRateBpm: List<SampleDataPoint<Double>> =
                    data.getData(DataType.HEART_RATE_BPM)
                if (heartRateBpm.isEmpty()) {
                    Log.d(TAG, "No heart rate data received.")
                } else {
                    Log.d(TAG, "Heart rate data received: ${heartRateBpm.last().value}")
                }
                trySend(MeasureMessage.MeasureData(heartRateBpm)).isSuccess
            }
        }

        Log.d(TAG, "Registering for heart rate data")
        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)

        // Register the heart beat sensor listener
        if (isHeartBeatSensorAvailable()) {
            Log.d(TAG, "Heart Beat Sensor available, registering listener")
            sensorManager.registerListener(
                this@HealthServicesManager,
                heartBeatSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        } else {
            Log.d(TAG, "Heart Beat Sensor not available")
        }

        // Launch a coroutine to collect from sensorChannel and emit to the flow
        val sensorJob = launch {
            for (message in sensorChannel) {
                trySend(message).isSuccess
            }
        }

        awaitClose {
            Log.d(TAG, "Unregistering for heart rate data")
            measureClient.unregisterMeasureCallback(DataType.HEART_RATE_BPM, callback)
            // Unregister the sensor listener
            sensorManager.unregisterListener(this@HealthServicesManager)
            // Close the sensorChannel
            sensorChannel.close()
            // Cancel the job
            sensorJob.cancel()
        }
    }

    // Implement SensorEventListener methods
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_HEART_BEAT) {
            val currentTimestamp = System.currentTimeMillis()
            if (lastBeatTimestamp != 0L) {
                val ibi = currentTimestamp - lastBeatTimestamp
                Log.d(TAG, "IBI: $ibi ms")
                // Send IBI data through the sensorChannel
                val success = sensorChannel.trySend(MeasureMessage.MeasureIBI(ibi)).isSuccess
                if (!success) {
                    Log.w(TAG, "Failed to send IBI data to channel")
                }
            }
            lastBeatTimestamp = currentTimestamp
            Log.d(TAG, "Heart Beat detected at $currentTimestamp")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if necessary
    }
}

sealed class MeasureMessage {
    class MeasureAvailability(val availability: DataTypeAvailability) : MeasureMessage()
    class MeasureData(val data: List<SampleDataPoint<Double>>) : MeasureMessage()
    class MeasureIBI(val ibi: Long) : MeasureMessage() // New data class for IBI
}
