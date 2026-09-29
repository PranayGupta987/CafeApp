package com.cafeapp.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeapp.core.common.AppliedOffer
import com.cafeapp.core.common.CartManager
import com.cafeapp.core.common.CartState
import com.cafeapp.core.network.OffersApi
import com.cafeapp.core.network.OrdersApi
import com.cafeapp.core.network.dto.OrderDto
import com.cafeapp.core.network.dto.OrderItemRequest
import com.cafeapp.core.network.dto.PlaceOrderRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartUiState(
    val promoInput: String = "",
    val isApplyingPromo: Boolean = false,
    val isPlacingOrder: Boolean = false,
    val placedOrder: OrderDto? = null,
    val errorMessage: String? = null,
    val userNotification: String? = null,
)

@HiltViewModel
class CustomerCartViewModel @Inject constructor(
    private val cartManager: CartManager,
    private val ordersApi: OrdersApi,
    private val offersApi: OffersApi,
) : ViewModel() {

    val cartState: StateFlow<CartState> = cartManager.cartState

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    fun onPromoInputChanged(code: String) {
        _uiState.update { it.copy(promoInput = code, errorMessage = null) }
    }

    fun onInstructionsChanged(instructions: String) {
        cartManager.setSpecialInstructions(instructions)
    }

    fun incrementItem(menuItemId: String) {
        cartManager.incrementItem(menuItemId)
    }

    fun decrementItem(menuItemId: String) {
        cartManager.decrementItem(menuItemId)
    }

    fun removeItem(menuItemId: String) {
        cartManager.removeItem(menuItemId)
    }

    fun applyPromo() {
        val code = _uiState.value.promoInput.trim().uppercase()
        if (code.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a promo code") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingPromo = true, errorMessage = null) }
            try {
                val response = offersApi.getActiveOffers()
                val offers = response.data ?: emptyList()
                val matched = offers.find { it.code.equals(code, ignoreCase = true) }

                if (matched != null) {
                    val subtotal = cartState.value.subtotalPaise
                    if (subtotal < matched.minOrderAmountPaise) {
                        val minOrderInRupees = matched.minOrderAmountPaise / 100
                        _uiState.update {
                            it.copy(
                                isApplyingPromo = false,
                                errorMessage = "Minimum order of ₹$minOrderInRupees required for this offer"
                            )
                        }
                        return@launch
                    }

                    val maxDisc = matched.maxDiscountAmountPaise
                    val discountPaise = if (matched.discountType == "PERCENTAGE") {
                        val calc = (subtotal * matched.discountValue) / 100
                        if (maxDisc != null && maxDisc > 0) minOf(calc, maxDisc) else calc
                    } else {
                        matched.discountValue
                    }

                    cartManager.applyOffer(
                        AppliedOffer(
                            code = matched.code,
                            title = matched.title,
                            discountType = matched.discountType,
                            discountValue = matched.discountValue,
                            discountPaise = discountPaise,
                        )
                    )
                    _uiState.update {
                        it.copy(
                            isApplyingPromo = false,
                            promoInput = "",
                            userNotification = "Offer ${matched.code} applied successfully!",
                            errorMessage = null,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isApplyingPromo = false,
                            errorMessage = "Invalid coupon code '$code'"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isApplyingPromo = false,
                        errorMessage = e.localizedMessage ?: "Failed to validate promo code"
                    )
                }
            }
        }
    }

    fun removePromo() {
        cartManager.removeOffer()
        _uiState.update { it.copy(userNotification = "Coupon removed") }
    }

    fun placeOrder(onSuccess: (OrderDto) -> Unit = {}) {
        val currentCart = cartState.value
        if (currentCart.items.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Your cart is empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPlacingOrder = true, errorMessage = null) }
            try {
                val request = PlaceOrderRequest(
                    tableId = currentCart.tableId,
                    tableNumber = currentCart.tableNumber,
                    items = currentCart.items.map { item ->
                        OrderItemRequest(
                            menuItemId = item.menuItemId,
                            quantity = item.quantity,
                            notes = item.notes
                        )
                    },
                    offerCode = currentCart.appliedOffer?.code,
                    specialInstructions = currentCart.specialInstructions?.ifBlank { null }
                )

                val response = ordersApi.placeOrder(request)
                val respData = response.data
                if (response.success && respData != null) {
                    val placed = respData
                    cartManager.clearCart()
                    _uiState.update {
                        it.copy(
                            isPlacingOrder = false,
                            placedOrder = placed,
                            userNotification = "Order #${placed.orderNumber} placed successfully!",
                            errorMessage = null,
                        )
                    }
                    onSuccess(placed)
                } else {
                    _uiState.update {
                        it.copy(
                            isPlacingOrder = false,
                            errorMessage = response.message ?: "Failed to place order"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPlacingOrder = false,
                        errorMessage = e.localizedMessage ?: "Network error while placing order"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, userNotification = null) }
    }

    fun resetOrderSuccess() {
        _uiState.update { it.copy(placedOrder = null) }
    }
}
