package com.spotifyhub.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.spotifyhub.ui.auth.AuthViewModel
import com.spotifyhub.ui.nowplaying.PlayerViewModel
import com.spotifyhub.ui.root.RootViewModel

class MainViewModelFactory(
    private val appGraph: AppGraph,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(RootViewModel::class.java) -> {
                RootViewModel(
                    authRepository = appGraph.authRepository,
                    connectivityMonitor = appGraph.connectivityMonitor,
                ) as T
            }

            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(appGraph.authRepository) as T
            }

            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> {
                PlayerViewModel(appGraph.playbackRepository) as T
            }

            else -> error("Unknown ViewModel: ${modelClass.name}")
        }
    }
}

