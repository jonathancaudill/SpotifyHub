package com.spotifyhub.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spotifyhub.auth.AuthUiState
import com.spotifyhub.auth.SessionState
import com.spotifyhub.auth.SpotifyAuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: SpotifyAuthRepository,
) : ViewModel() {
    val uiState = authRepository.sessionState
        .map { state ->
            when (state) {
                SessionState.Loading -> AuthUiState(isBusy = true, isSignedIn = false)
                SessionState.SigningIn -> AuthUiState(isBusy = true, isSignedIn = false)
                SessionState.SignedOut -> AuthUiState(isBusy = false, isSignedIn = false)
                is SessionState.Ready -> AuthUiState(isBusy = false, isSignedIn = true)
                is SessionState.Error -> AuthUiState(
                    isBusy = false,
                    isSignedIn = false,
                    message = state.message,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthUiState(isBusy = true, isSignedIn = false),
        )

    fun connect(context: Context) {
        viewModelScope.launch {
            authRepository.beginLogin(context)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }
}

