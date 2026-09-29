package com.cafeapp.feature.cafe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeapp.core.common.CartManager
import com.cafeapp.core.datastore.SessionPreferences
import com.cafeapp.core.network.AuthApi
import com.cafeapp.core.network.CafeApi
import com.cafeapp.core.network.TableSessionsApi
import com.cafeapp.core.network.dto.CafeDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerProfileUiState(
    val isLoading: Boolean = false,
    val isLoggingOut: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null,
    val userRole: String? = null,
    val userId: String? = null,
    val tableNumber: Int? = null,
    val cafe: CafeDto? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class CustomerProfileViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val cafeApi: CafeApi,
    private val tableSessionsApi: TableSessionsApi,
    private val sessionPreferences: SessionPreferences,
    private val cartManager: CartManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerProfileUiState(isLoading = true))
    val uiState: StateFlow<CustomerProfileUiState> = _uiState.asStateFlow()

    init {
        observeSession()
        loadProfileData()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionPreferences.userName.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
        viewModelScope.launch {
            sessionPreferences.userEmail.collect { email ->
                _uiState.update { it.copy(userEmail = email) }
            }
        }
        viewModelScope.launch {
            sessionPreferences.userRole.collect { role ->
                _uiState.update { it.copy(userRole = role) }
            }
        }
        viewModelScope.launch {
            sessionPreferences.userId.collect { id ->
                _uiState.update { it.copy(userId = id) }
            }
        }
        viewModelScope.launch {
            sessionPreferences.currentTableNumber.collect { tableNum ->
                _uiState.update { it.copy(tableNumber = tableNum) }
            }
        }
    }

    fun loadProfileData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Fetch me
                try {
                    val meResp = authApi.getMe()
                    val user = meResp.user
                    _uiState.update {
                        it.copy(
                            userName = user.name ?: it.userName,
                            userEmail = user.email,
                            userRole = user.role,
                            userId = user.id,
                        )
                    }
                } catch (_: Exception) {}

                // Fetch Cafe profile
                val cafeResp = try { cafeApi.getCafeProfile() } catch (e: Exception) { null }
                val cafe = cafeResp?.data

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cafe = cafe,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load profile",
                    )
                }
            }
        }
    }

    fun leaveTable() {
        viewModelScope.launch {
            try {
                val activeSessionResp = tableSessionsApi.getActiveSession()
                val activeSession = activeSessionResp.data
                if (activeSession != null) {
                    tableSessionsApi.checkOut(activeSession.id)
                }
            } catch (_: Exception) {}
            sessionPreferences.clearTable()
            cartManager.clearTable()
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            try {
                try {
                    authApi.logout()
                } catch (_: Exception) {}

                sessionPreferences.clearAll()
                cartManager.clearCart()
                _uiState.update { it.copy(isLoggingOut = false) }
                onSuccess()
            } catch (e: Exception) {
                sessionPreferences.clearAll()
                cartManager.clearCart()
                _uiState.update { it.copy(isLoggingOut = false) }
                onSuccess()
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
