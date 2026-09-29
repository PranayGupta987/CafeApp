package com.cafeapp.core.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class CartItem(
    val menuItemId: String,
    val name: String,
    val pricePaise: Int,
    val isVeg: Boolean = true,
    val quantity: Int = 1,
    val notes: String? = null,
)

data class AppliedOffer(
    val code: String,
    val title: String? = null,
    val discountType: String,
    val discountValue: Int,
    val discountPaise: Int,
)

data class CartState(
    val items: List<CartItem> = emptyList(),
    val appliedOffer: AppliedOffer? = null,
    val specialInstructions: String? = null,
    val tableId: String? = null,
    val tableNumber: Int? = null,
) {
    val subtotalPaise: Int
        get() = items.sumOf { it.pricePaise * it.quantity }

    val discountPaise: Int
        get() = appliedOffer?.discountPaise ?: 0

    val subtotalAfterDiscountPaise: Int
        get() = maxOf(0, subtotalPaise - discountPaise)

    val gstPaise: Int
        get() = (subtotalAfterDiscountPaise * 5) / 100

    val taxPaise: Int
        get() = gstPaise

    val totalPaise: Int
        get() = subtotalAfterDiscountPaise + gstPaise

    val totalItemCount: Int
        get() = items.sumOf { it.quantity }
}

@Singleton
class CartManager @Inject constructor() {
    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    fun addItem(
        menuItemId: String,
        name: String,
        pricePaise: Int,
        isVeg: Boolean = true,
        notes: String? = null
    ) {
        _cartState.update { current ->
            val existingIndex = current.items.indexOfFirst { it.menuItemId == menuItemId }
            val newItems = if (existingIndex >= 0) {
                current.items.mapIndexed { idx, item ->
                    if (idx == existingIndex) item.copy(quantity = item.quantity + 1) else item
                }
            } else {
                current.items + CartItem(
                    menuItemId = menuItemId,
                    name = name,
                    pricePaise = pricePaise,
                    isVeg = isVeg,
                    quantity = 1,
                    notes = notes
                )
            }
            current.copy(items = newItems)
        }
    }

    fun incrementItem(menuItemId: String) {
        _cartState.update { current ->
            val existingIndex = current.items.indexOfFirst { it.menuItemId == menuItemId }
            if (existingIndex < 0) return@update current
            val newItems = current.items.mapIndexed { idx, item ->
                if (idx == existingIndex) item.copy(quantity = item.quantity + 1) else item
            }
            current.copy(items = newItems)
        }
    }

    fun decrementItem(menuItemId: String) {
        _cartState.update { current ->
            val existingIndex = current.items.indexOfFirst { it.menuItemId == menuItemId }
            if (existingIndex < 0) return@update current
            val item = current.items[existingIndex]
            val newItems = if (item.quantity > 1) {
                current.items.mapIndexed { idx, i ->
                    if (idx == existingIndex) i.copy(quantity = i.quantity - 1) else i
                }
            } else {
                current.items.filterNot { it.menuItemId == menuItemId }
            }
            current.copy(items = newItems)
        }
    }

    fun removeItem(menuItemId: String) {
        _cartState.update { current ->
            current.copy(items = current.items.filterNot { it.menuItemId == menuItemId })
        }
    }

    fun applyOffer(offer: AppliedOffer) {
        _cartState.update { current ->
            current.copy(appliedOffer = offer)
        }
    }

    fun removeOffer() {
        _cartState.update { current ->
            current.copy(appliedOffer = null)
        }
    }

    fun setTable(tableId: String?, tableNumber: Int?) {
        _cartState.update { current ->
            current.copy(tableId = tableId, tableNumber = tableNumber)
        }
    }

    fun clearTable() {
        _cartState.update { current ->
            current.copy(tableId = null, tableNumber = null)
        }
    }

    fun setSpecialInstructions(instructions: String?) {
        _cartState.update { current ->
            current.copy(specialInstructions = instructions)
        }
    }

    fun clearCart() {
        _cartState.value = CartState(
            tableId = _cartState.value.tableId,
            tableNumber = _cartState.value.tableNumber,
        )
    }
}
