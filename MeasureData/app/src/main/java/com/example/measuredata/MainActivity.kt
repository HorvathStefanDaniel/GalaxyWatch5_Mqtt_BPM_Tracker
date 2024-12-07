package com.example.measuredata

import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    private lateinit var statusText: TextView
    private lateinit var heartRateText: TextView
    private lateinit var ibiTextView: TextView
    private lateinit var startMeasureButton: Button

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the screen on while the app is running
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)

        // Initialize views
        statusText = findViewById(R.id.statusText)
        heartRateText = findViewById(R.id.heartRateText)
        ibiTextView = findViewById(R.id.ibiTextView)

        // Observe IBI Value
        lifecycleScope.launch {
            viewModel.ibiValue.collect { ibi ->
                ibi?.let {
                    // Update UI to display IBI
                    ibiTextView.text = "$ibi ms"
                }
            }
        }

        // Observe Heart Rate BPM
        lifecycleScope.launch {
            viewModel.heartRateBpm.collect { bpm ->
                heartRateText.text = "${bpm.toInt()}"
            }
        }

        // Observe UI State
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Startup -> statusText.text = "Starting up..."
                    is UiState.HeartRateAvailable -> statusText.text = "Heart rate sensor available."
                    is UiState.HeartRateNotAvailable -> statusText.text = "Heart rate sensor not available."
                }
            }
        }

        //just start measuring
        //viewModel.measureHeartRate()
    }
}
