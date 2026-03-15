package com.spotifyhub.auth

data class AuthUiState(
    val isBusy: Boolean,
    val isSignedIn: Boolean,
    val message: String? = null,
)

