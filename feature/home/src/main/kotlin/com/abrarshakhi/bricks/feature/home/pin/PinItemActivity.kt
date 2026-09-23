package com.abrarshakhi.bricks.feature.home.pin

import android.content.pm.LauncherApps
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.abrarshakhi.bricks.feature.home.R
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class PinItemActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val request = runCatching { getSystemService(LauncherApps::class.java).getPinItemRequest(intent) }.getOrNull()
        val handle = PlatformPinItemRequest(this, request)
        setContent {
            PinItemRoute(
                onFinished = ::onFinished,
                viewModel = koinViewModel { parametersOf(handle) },
            )
        }
    }

    private fun onFinished(outcome: PinOutcome) {
        when (outcome) {
            PinOutcome.Added -> Toast.makeText(this, R.string.home_pin_added, Toast.LENGTH_SHORT).show()
            PinOutcome.Failed -> Toast.makeText(this, R.string.home_pin_failed, Toast.LENGTH_SHORT).show()
            PinOutcome.Cancelled -> Unit
        }
        finish()
    }
}
