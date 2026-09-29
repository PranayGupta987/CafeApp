package com.cafeapp.navigation.placeholder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeapp.core.network.HealthApi
import com.cafeapp.core.network.HealthResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HealthState {
    data object Idle : HealthState
    data object Loading : HealthState
    data class Success(val response: HealthResponse) : HealthState
    data class Error(val message: String) : HealthState
}

@HiltViewModel
class DevelopmentFoundationViewModel @Inject constructor(
    private val healthApi: HealthApi,
) : ViewModel() {

    private val _healthState = MutableStateFlow<HealthState>(HealthState.Idle)
    val healthState: StateFlow<HealthState> = _healthState.asStateFlow()

    init {
        checkBackendHealth()
    }

    fun checkBackendHealth() {
        viewModelScope.launch {
            _healthState.value = HealthState.Loading
            try {
                val response = healthApi.checkHealth()
                _healthState.value = HealthState.Success(response)
            } catch (e: Exception) {
                _healthState.value = HealthState.Error(e.localizedMessage ?: "Failed to connect to backend")
            }
        }
    }
}
