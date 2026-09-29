package com.cafeapp.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeapp.core.common.CafeResult
import com.cafeapp.feature.auth.domain.usecase.CheckSessionUseCase
import com.cafeapp.feature.auth.domain.usecase.GetMeUseCase
import com.cafeapp.feature.auth.domain.usecase.LoginUseCase
import com.cafeapp.feature.auth.domain.usecase.LogoutUseCase
import com.cafeapp.feature.auth.domain.usecase.RegisterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getMeUseCase: GetMeUseCase,
    private val checkSessionUseCase: CheckSessionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.LoginClicked -> login(event.email, event.password)
            is AuthEvent.RegisterClicked -> register(
                event.name,
                event.email,
                event.password,
                event.confirmPassword,
            )
            AuthEvent.LogoutClicked -> logout()
            AuthEvent.ClearError -> _state.value = AuthState.Idle
            AuthEvent.CheckSession -> checkSession()
        }
    }

    private fun login(email: String, password: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = loginUseCase(email, password)) {
                is CafeResult.Success -> {
                    _state.value = AuthState.Authenticated(result.data)
                }
                is CafeResult.Error -> {
                    _state.value = AuthState.Error(result.message ?: "Authentication failed")
                }
                CafeResult.Loading -> {
                    _state.value = AuthState.Loading
                }
            }
        }
    }

    private fun register(name: String, email: String, password: String, confirmPass: String) {
        if (password != confirmPass) {
            _state.value = AuthState.Error("Passwords do not match")
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = registerUseCase(name, email, password)) {
                is CafeResult.Success -> {
                    _state.value = AuthState.Authenticated(result.data)
                }
                is CafeResult.Error -> {
                    _state.value = AuthState.Error(result.message ?: "Registration failed")
                }
                CafeResult.Loading -> {
                    _state.value = AuthState.Loading
                }
            }
        }
    }

    private fun logout() {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            logoutUseCase()
            _state.value = AuthState.LoggedOut
        }
    }

    private fun checkSession() {
        viewModelScope.launch {
            val hasSession = checkSessionUseCase()
            if (hasSession) {
                when (val userResult = getMeUseCase()) {
                    is CafeResult.Success -> {
                        _state.value = AuthState.Authenticated(userResult.data)
                    }
                    else -> {
                        _state.value = AuthState.Idle
                    }
                }
            } else {
                _state.value = AuthState.Idle
            }
        }
    }
}
