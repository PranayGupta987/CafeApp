package com.cafeapp.feature.auth.ui

import com.cafeapp.feature.auth.domain.AuthUser

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
    data object LoggedOut : AuthState
    data class Error(val message: String) : AuthState
}

sealed interface AuthEvent {
    data class LoginClicked(val email: String, val password: String) : AuthEvent
    data class RegisterClicked(
        val name: String,
        val email: String,
        val password: String,
        val confirmPassword: String,
    ) : AuthEvent
    data object LogoutClicked : AuthEvent
    data object ClearError : AuthEvent
    data object CheckSession : AuthEvent
}
