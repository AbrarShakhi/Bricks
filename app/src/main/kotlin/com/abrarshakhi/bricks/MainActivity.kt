package com.abrarshakhi.bricks

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abrarshakhi.bricks.core.ui.theme.LauncherTheme
import com.abrarshakhi.bricks.core.ui.theme.isDark
import com.abrarshakhi.bricks.core.ui.util.HomeButtonEvents
import com.abrarshakhi.bricks.core.ui.widget.ActivityResultBridge
import com.abrarshakhi.bricks.core.ui.widget.LauncherWidgetHost
import com.abrarshakhi.bricks.navigation.LauncherNavHost
import com.abrarshakhi.bricks.shell.AppShellViewModel
import org.koin.android.ext.android.inject
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    private val widgetHost: LauncherWidgetHost by inject()
    private val homeButtons: HomeButtonEvents by inject()
    private val resultBridge: ActivityResultBridge by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            val shell: AppShellViewModel = koinViewModel()
            val appearance by shell.appearance.collectAsStateWithLifecycle()
            LauncherTheme(appearance = appearance) {
                LauncherNavHost(darkTheme = appearance.themeMode.isDark())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHost.startListening()
    }

    override fun onStop() {
        super.onStop()
        widgetHost.stopListening()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            val broughtToFront = (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0
            homeButtons.onHomePressed(alreadyVisible = hasWindowFocus() && !broughtToFront)
        }
    }

    @Deprecated("Needed for AppWidgetHost configuration results")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        resultBridge.dispatch(requestCode, resultCode)
    }
}
