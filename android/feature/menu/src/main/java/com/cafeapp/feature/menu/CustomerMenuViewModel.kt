package com.cafeapp.feature.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeapp.core.common.CartManager
import com.cafeapp.core.common.CartState
import com.cafeapp.core.network.MenuApi
import com.cafeapp.core.network.dto.MenuCategoryDto
import com.cafeapp.core.network.dto.MenuItemDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuUiState(
    val isLoading: Boolean = false,
    val categories: List<MenuCategoryDto> = emptyList(),
    val selectedCategoryId: String? = null,
    val allItems: List<MenuItemDto> = emptyList(),
    val searchQuery: String = "",
    val isVegOnly: Boolean = false,
    val errorMessage: String? = null,
) {
    val filteredItems: List<MenuItemDto>
        get() = allItems.filter { item ->
            val matchesCategory = selectedCategoryId == null || item.categoryId == selectedCategoryId
            val matchesVeg = !isVegOnly || (item.isVeg == true)
            val matchesSearch = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    (item.description?.contains(searchQuery, ignoreCase = true) == true)
            matchesCategory && matchesVeg && matchesSearch
        }
}

@HiltViewModel
class CustomerMenuViewModel @Inject constructor(
    private val menuApi: MenuApi,
    val cartManager: CartManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuUiState(isLoading = true))
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    val cartState: StateFlow<CartState> = cartManager.cartState

    init {
        loadMenuData()
    }

    fun loadMenuData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val catResponse = menuApi.getCategories()
                val itemsResponse = menuApi.getMenuItems()

                val categories = catResponse.data ?: emptyList()
                val items = itemsResponse.data ?: emptyList()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        categories = categories,
                        allItems = items,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load menu"
                    )
                }
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleVegOnly() {
        _uiState.update { it.copy(isVegOnly = !it.isVegOnly) }
    }

    fun addToCart(item: MenuItemDto) {
        cartManager.addItem(
            menuItemId = item.id,
            name = item.name,
            pricePaise = item.pricePaise,
            isVeg = item.isVeg ?: true,
        )
    }

    fun decrementCartItem(itemId: String) {
        cartManager.decrementItem(itemId)
    }
}
