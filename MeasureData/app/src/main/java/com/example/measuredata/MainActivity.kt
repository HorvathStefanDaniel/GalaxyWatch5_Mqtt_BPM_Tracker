package com.example.measuredata

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.lifecycleScope
import com.example.measuredata.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
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

        // **Initialize the binding first**
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

        // Observe heart rate and UI state
        lifecycleScope.launch {
            viewModel.heartRateBpm.collect { bpm ->
                binding.heartRateText.text = "Heart Rate: $bpm bpm"
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Startup -> binding.statusText.text = "Starting up..."
                    is UiState.HeartRateAvailable -> binding.statusText.text = "Heart rate sensor available."
                    is UiState.HeartRateNotAvailable -> binding.statusText.text = "Heart rate sensor not available."
                    UiState.ConnectedToMQTT -> {
                        binding.statusText.text = "Connected to MQTT"
                        Log.d("MainActivity", "MQTT connection established")
                        // Reflect the online state in the UI
                        binding.toggleModeButton.isChecked = true
                    }
                    UiState.ConnectingToMQTT -> {
                        binding.statusText.text = "Connecting to MQTT..."
                        Log.d("MainActivity", "Attempting to connect to MQTT")
                        // Start MqttConnectionActivity
                        startActivity(Intent(this@MainActivity, MqttConnectionActivity::class.java))
                    }
                    UiState.FailedToConnectMQTT -> {
                        binding.statusText.text = "Failed to connect to MQTT"
                        Log.e("MainActivity", "MQTT connection failed")
                        // Reflect the offline state in the UI
                        binding.toggleModeButton.isChecked = false
                    }
                }
            }
        }

        binding.startMeasureButton.setOnClickListener {
            startHeartRateMeasurement()
        }

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

        binding.updateMqttButton.setOnClickListener {
            val serverUri = binding.ipAddressInput.text.toString()
            val port = binding.portInput.text.toString().toIntOrNull() ?: 1883 // Default to port 1883 if not specified
            val username = binding.usernameInput.text.toString().ifEmpty { null }
            val password = binding.passwordInput.text.toString().ifEmpty { null }

            viewModel.updateMqttDetails(serverUri, username, password, port)

            // Save the new settings to SharedPreferences
            val sharedPreferences = getSharedPreferences("mqtt_prefs", MODE_PRIVATE)
            sharedPreferences.edit()
                .putString("serverUri", serverUri)
                .putInt("port", port)
                .putString("username", username)
                .putString("password", password)
                .apply()
        }
    }

    // Function to check and request the BODY_SENSORS permission
    private fun checkAndRequestPermission() {
        val permission = android.Manifest.permission.BODY_SENSORS

        when (ContextCompat.checkSelfPermission(this, permission)) {
            PermissionChecker.PERMISSION_GRANTED -> {
                Log.d("MainActivity", "BODY_SENSORS permission already granted")
                startHeartRateMeasurement()
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
