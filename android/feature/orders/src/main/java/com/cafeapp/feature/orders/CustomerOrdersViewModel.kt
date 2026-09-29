package com.cafeapp.feature.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeapp.core.network.OrdersApi
import com.cafeapp.core.network.dto.OrderDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerOrdersUiState(
    val isLoading: Boolean = false,
    val orders: List<OrderDto> = emptyList(),
    val activeTab: String = "ALL", // ALL, ACTIVE, COMPLETED
    val errorMessage: String? = null,
)

@HiltViewModel
class CustomerOrdersViewModel @Inject constructor(
    private val ordersApi: OrdersApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerOrdersUiState(isLoading = true))
    val uiState: StateFlow<CustomerOrdersUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = ordersApi.getOrders()
                val orders = response.data ?: emptyList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        orders = orders,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load orders"
                    )
                }
            }
        }
    }

    fun setActiveTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
