package com.cafeapp.navigation

import androidx.lifecycle.ViewModel
import com.cafeapp.core.common.CartManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CustomerMainViewModel @Inject constructor(
    val cartManager: CartManager,
) : ViewModel()
