package com.cafeapp.feature.cafe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeapp.core.common.AppliedOffer
import com.cafeapp.core.common.CartManager
import com.cafeapp.core.common.CartState
import com.cafeapp.core.datastore.SessionPreferences
import com.cafeapp.core.network.CafeApi
import com.cafeapp.core.network.MenuApi
import com.cafeapp.core.network.OffersApi
import com.cafeapp.core.network.dto.CafeDto
import com.cafeapp.core.network.dto.MenuItemDto
import com.cafeapp.core.network.dto.OfferDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerHomeUiState(
    val isLoading: Boolean = false,
    val cafe: CafeDto? = null,
    val offers: List<OfferDto> = emptyList(),
    val popularItems: List<MenuItemDto> = emptyList(),
    val userName: String? = null,
    val tableNumber: Int? = null,
    val errorMessage: String? = null,
    val notificationMessage: String? = null,
)

@HiltViewModel
class CustomerHomeViewModel @Inject constructor(
    private val cafeApi: CafeApi,
    private val menuApi: MenuApi,
    private val offersApi: OffersApi,
    private val sessionPreferences: SessionPreferences,
    private val cartManager: CartManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomerHomeUiState(isLoading = true))
    val uiState: StateFlow<CustomerHomeUiState> = _uiState.asStateFlow()

    val cartState: StateFlow<CartState> = cartManager.cartState

    init {
        observeSession()
        loadHomeData()
    }

    private fun observeSession() {
        viewModelScope.launch {
            sessionPreferences.userName.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
        viewModelScope.launch {
            sessionPreferences.currentTableNumber.collect { tableNum ->
                _uiState.update { it.copy(tableNumber = tableNum) }
            }
        }
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Fetch Cafe profile
                val cafeResp = try { cafeApi.getCafeProfile() } catch (e: Exception) { null }
                val cafe = cafeResp?.data

                // Fetch Offers
                val offersResp = try { offersApi.getActiveOffers() } catch (e: Exception) { null }
                val offers = offersResp?.data ?: emptyList()

                // Fetch Popular Items
                val menuResp = try { menuApi.getMenuItems(isAvailable = true) } catch (e: Exception) { null }
                val popular = menuResp?.data?.take(4) ?: emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cafe = cafe,
                        offers = offers,
                        popularItems = popular,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load cafe details"
                    )
                }
            }
        }
    }

    fun addToCart(item: MenuItemDto) {
        cartManager.addItem(
            menuItemId = item.id,
            name = item.name,
            pricePaise = item.pricePaise,
            isVeg = item.isVeg
        )
        _uiState.update { it.copy(notificationMessage = "Added ${item.name} to cart") }
    }

    fun applyOffer(offer: OfferDto) {
        val currentSubtotal = cartManager.cartState.value.subtotalPaise
        val maxDisc = offer.maxDiscountAmountPaise
        val discountPaise = if (offer.discountType == "PERCENTAGE") {
            val calc = (currentSubtotal * offer.discountValue) / 100
            if (maxDisc != null && maxDisc > 0) minOf(calc, maxDisc) else calc
        } else {
            offer.discountValue
        }

        cartManager.applyOffer(
            AppliedOffer(
                code = offer.code,
                title = offer.title,
                discountType = offer.discountType,
                discountValue = offer.discountValue,
                discountPaise = discountPaise,
            )
        )
        _uiState.update { it.copy(notificationMessage = "Promo code ${offer.code} applied!") }
    }

    fun clearNotification() {
        _uiState.update { it.copy(notificationMessage = null, errorMessage = null) }
    }
}
