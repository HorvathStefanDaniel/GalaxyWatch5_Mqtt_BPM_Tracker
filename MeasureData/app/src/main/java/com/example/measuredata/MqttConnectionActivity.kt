package com.example.measuredata

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MqttConnectionActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var statusTextView: TextView
    private val viewModel: MainViewModel by viewModels()
    //private var status = "."

    // Simulate the UI showing for a while even after success/failure
    private suspend fun delayAndCloseActivity() {
        runOnUiThread {
            progressBar.visibility = View.GONE
        }
        // Add a delay before closing the activity
        kotlinx.coroutines.delay(500) // Delay for 1.5 seconds to ensure visibility
        finish() // Close MqttConnectionActivity and return to MainActivity
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt_connection)

        // Prevent the screen from going to sleep while this activity is active
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize views
        progressBar = findViewById(R.id.progressBar)
        statusTextView = findViewById(R.id.statusTextView)

        // Set the progress bar as determinate for gradual progress updates
        progressBar.forceLayout();
        progressBar.invalidate();
        progressBar.setProgress(0, false);

        // Observe the UI state for MQTT connection and update the UI accordingly
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                when (uiState) {
                    is UiState.ConnectingToMQTT -> {
                        runOnUiThread {
                            progressBar.visibility = View.VISIBLE
                            statusTextView.text = "Connecting to MQTT..."

                            // Set progress to 10% after a short delay to simulate activity
                            progressBar.progress = 0
                        }
                    }
                    is UiState.ConnectedToMQTT -> {
                        runOnUiThread {
                            statusTextView.text = "Connected to MQTT"
                        }
                        delayAndCloseActivity() // Delay to show the success message
                    }
                    is UiState.FailedToConnectMQTT -> {
                        runOnUiThread {
                            statusTextView.text = "Failed to connect to MQTT"
                        }
                        delayAndCloseActivity() // Delay to show the failure message
                    }
                    else -> {
                        runOnUiThread {
                            //progressBar.visibility = View.GONE
                            //statusTextView.text = status
                            //status += "."
                        }
                    }
                }
            }
        }
    }


}



