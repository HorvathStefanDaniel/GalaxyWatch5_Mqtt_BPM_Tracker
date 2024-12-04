package com.example.measuredata

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.lifecycleScope
import com.example.measuredata.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    // Declare the permission launcher to handle the BODY_SENSORS permission request
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the screen on while the app is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Initialize the binding first
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the permission launcher
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("MainActivity", "Permission granted for BODY_SENSORS")
                startHeartRateMeasurement()
            } else {
                Log.e("MainActivity", "Permission denied for BODY_SENSORS")
                binding.statusText.text = "Permission required to measure heart rate"
            }
        }

        // Check and request permission in onStart
        checkAndRequestPermission()

        // Observe IBI Value
        lifecycleScope.launch {
            viewModel.ibiValue.collect { ibi ->
                ibi?.let {
                    // Update UI to display IBI
                    binding.ibiTextView.text = "IBI: $ibi ms"
                }
            }
        }

        // Observe Heart Rate BPM
        lifecycleScope.launch {
            viewModel.heartRateBpm.collect { bpm ->
                binding.heartRateText.text = "Heart Rate: $bpm bpm"
            }
        }

        // Observe UI State
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Startup -> binding.statusText.text = "Starting up..."
                    is UiState.HeartRateAvailable -> binding.statusText.text = "Heart rate sensor available."
                    is UiState.HeartRateNotAvailable -> binding.statusText.text = "Heart rate sensor not available."
                    is UiState.ConnectingToMQTT -> {
                        binding.statusText.text = "Connecting to MQTT..."
                        Log.d("MainActivity", "Attempting to connect to MQTT")
                        // Optionally start an activity or show a progress indicator
                    }
                    is UiState.ConnectedToMQTT -> {
                        binding.statusText.text = "Connected to MQTT"
                        Log.d("MainActivity", "MQTT connection established")
                        // Reflect the online state in the UI
                        binding.toggleModeButton.isChecked = true
                    }
                    is UiState.FailedToConnectMQTT -> {
                        binding.statusText.text = "Failed to connect to MQTT"
                        Log.e("MainActivity", "MQTT connection failed")
                        // Reflect the offline state in the UI
                        binding.toggleModeButton.isChecked = false
                    }
                }
            }
        }



        // Start Measuring Heart Rate Button
        binding.startMeasureButton.setOnClickListener {
            startHeartRateMeasurement()
        }

        // Toggle Online/Offline Mode Button
        binding.toggleModeButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Online mode (start MQTT)
                viewModel.setupMqtt() // This will trigger the UI state change to ConnectingToMQTT
            } else {
                // Offline mode (disconnect MQTT)
                viewModel.stopMqtt()
            }
            Log.d("MainActivity", "Online mode toggled: $isChecked")
        }
    }

    // Function to check and request the BODY_SENSORS permission
    private fun checkAndRequestPermission() {
        val permission = android.Manifest.permission.BODY_SENSORS

        when (ContextCompat.checkSelfPermission(this, permission)) {
            PermissionChecker.PERMISSION_GRANTED -> {
                Log.d("MainActivity", "BODY_SENSORS permission already granted")
                //startHeartRateMeasurement()
            }
            else -> {
                Log.d("MainActivity", "Requesting BODY_SENSORS permission")
                permissionLauncher.launch(permission)
            }
        }
    }

    // Function to start heart rate measurement
    private fun startHeartRateMeasurement() {
        lifecycleScope.launch {
            viewModel.measureHeartRate()
        }
    }
}
