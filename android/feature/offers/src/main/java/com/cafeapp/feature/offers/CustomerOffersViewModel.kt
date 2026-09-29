package com.cafeapp.feature.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeapp.core.common.AppliedOffer
import com.cafeapp.core.common.CartManager
import com.cafeapp.core.common.CartState
import com.cafeapp.core.network.OffersApi
import com.cafeapp.core.network.dto.OfferDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OffersUiState(
    val isLoading: Boolean = false,
    val offers: List<OfferDto> = emptyList(),
    val selectedOfferForDetails: OfferDto? = null,
    val errorMessage: String? = null,
    val userNotification: String? = null,
)

@HiltViewModel
class CustomerOffersViewModel @Inject constructor(
    private val offersApi: OffersApi,
    val cartManager: CartManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OffersUiState(isLoading = true))
    val uiState: StateFlow<OffersUiState> = _uiState.asStateFlow()

    val cartState: StateFlow<CartState> = cartManager.cartState

    init {
        loadOffers()
    }

    fun loadOffers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = offersApi.getActiveOffers()
                val offers = response.data ?: emptyList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        offers = offers,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load offers"
                    )
                }
            }
        }
    }

    fun showOfferDetails(offer: OfferDto) {
        _uiState.update { it.copy(selectedOfferForDetails = offer) }
    }

    fun dismissOfferDetails() {
        _uiState.update { it.copy(selectedOfferForDetails = null) }
    }

    fun onCodeCopied(code: String) {
        _uiState.update {
            it.copy(userNotification = "Code '$code' copied to clipboard!")
        }
    }

    fun applyOffer(offer: OfferDto) {
        val currentSubtotal = cartManager.cartState.value.subtotalPaise
        if (currentSubtotal < offer.minOrderAmountPaise) {
            _uiState.update {
                it.copy(userNotification = "Minimum order of ₹${offer.minOrderAmountPaise / 100} required for code ${offer.code}")
            }
            return
        }

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

        _uiState.update {
            it.copy(userNotification = "Coupon ${offer.code} applied successfully!")
        }
    }

    fun clearNotification() {
        _uiState.update { it.copy(userNotification = null) }
    }
}
