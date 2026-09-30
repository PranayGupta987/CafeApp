package com.cafeapp.feature.kitchen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeapp.core.network.OrdersApi
import com.cafeapp.core.network.dto.OrderDto
import com.cafeapp.core.network.dto.UpdateOrderStatusRequest
import com.cafeapp.core.realtime.OrderNewEvent
import com.cafeapp.core.realtime.OrderStatusUpdateEvent
import com.cafeapp.core.realtime.SocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KitchenUiState(
    val isLoading: Boolean = false,
    val orders: List<OrderDto> = emptyList(),
    val activeTab: String = "ACTIVE", // ACTIVE, PLACED, PREPARING, READY, SERVED, ALL
    val isSocketConnected: Boolean = false,
    val updatingOrderId: String? = null,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

sealed interface KitchenUiEvent {
    data class NewOrderAlert(val orderNumber: Int, val tableNumber: Int?) : KitchenUiEvent
}

@HiltViewModel
class KitchenViewModel @Inject constructor(
    private val ordersApi: OrdersApi,
    private val socketManager: SocketManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(KitchenUiState(isLoading = true))
    val uiState: StateFlow<KitchenUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<KitchenUiEvent>(extraBufferCapacity = 16)
    val uiEvents: SharedFlow<KitchenUiEvent> = _uiEvents.asSharedFlow()

    init {
        loadOrders()
        initRealtime()
    }

    private fun initRealtime() {
        socketManager.connect()

        viewModelScope.launch {
            socketManager.isConnected.collect { connected ->
                _uiState.update { it.copy(isSocketConnected = connected) }
            }
        }

        viewModelScope.launch {
            socketManager.orderNewEvents.collect { event ->
                _uiEvents.emit(KitchenUiEvent.NewOrderAlert(event.orderNumber, event.tableNumber))
                _uiState.update { it.copy(infoMessage = "New incoming order #${event.orderNumber}!") }
                loadOrders(silent = true)
            }
        }

        viewModelScope.launch {
            socketManager.orderStatusUpdates.collect { update ->
                _uiState.update { current ->
                    val updated = current.orders.map { order ->
                        if (order.id == update.orderId) {
                            order.copy(status = update.status)
                        } else {
                            order
                        }
                    }
                    current.copy(orders = updated)
                }
                loadOrders(silent = true)
            }
        }
    }

    fun loadOrders(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            try {
                val response = ordersApi.getOrders()
                val list = response.data ?: emptyList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        orders = list,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                if (!silent) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.localizedMessage ?: "Failed to fetch orders"
                        )
                    }
                }
            }
        }
    }

    fun setActiveTab(tab: String) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun updateStatus(orderId: String, nextStatus: String, notes: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(updatingOrderId = orderId, errorMessage = null) }
            try {
                val response = ordersApi.updateOrderStatus(
                    id = orderId,
                    request = UpdateOrderStatusRequest(status = nextStatus, notes = notes)
                )
                val updatedOrder = response.data
                if (response.success && updatedOrder != null) {
                    _uiState.update { current ->
                        val updatedList = current.orders.map {
                            if (it.id == orderId) updatedOrder else it
                        }
                        current.copy(
                            updatingOrderId = null,
                            orders = updatedList,
                            infoMessage = "Order #${updatedOrder.orderNumber} updated to ${updatedOrder.status}"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            updatingOrderId = null,
                            errorMessage = response.message ?: "Failed to update order status"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        updatingOrderId = null,
                        errorMessage = e.localizedMessage ?: "Error updating status"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }
}
