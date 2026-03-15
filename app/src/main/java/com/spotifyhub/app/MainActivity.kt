package com.spotifyhub.app

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.spotifyhub.system.kiosk.SystemUiController
import com.spotifyhub.theme.SpotifyHubTheme
import com.spotifyhub.ui.auth.AuthViewModel
import com.spotifyhub.ui.nowplaying.PlayerViewModel
import com.spotifyhub.ui.root.RootScreen
import com.spotifyhub.ui.root.RootViewModel

class MainActivity : ComponentActivity() {
    private val appGraph by lazy { (application as SpotifyHubApp).appGraph }
    private val viewModelFactory by lazy { MainViewModelFactory(appGraph) }

    private val rootViewModel: RootViewModel by viewModels { viewModelFactory }
    private val authViewModel: AuthViewModel by viewModels { viewModelFactory }
    private val playerViewModel: PlayerViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SpotifyHubTheme {
                RootScreen(
                    rootViewModel = rootViewModel,
                    authViewModel = authViewModel,
                    playerViewModel = playerViewModel,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SystemUiController.enterImmersive(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            SystemUiController.enterImmersive(this)
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (appGraph.inputRouter.onKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

