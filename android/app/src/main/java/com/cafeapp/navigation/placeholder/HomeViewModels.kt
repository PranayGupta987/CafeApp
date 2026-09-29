package com.cafeapp.navigation.placeholder

import androidx.lifecycle.ViewModel
import com.cafeapp.core.datastore.SessionPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CustomerHomeViewModel @Inject constructor(
    val sessionPreferences: SessionPreferences,
) : ViewModel()

@HiltViewModel
class CafeHomeViewModel @Inject constructor(
    val sessionPreferences: SessionPreferences,
) : ViewModel()

@HiltViewModel
class KitchenHomeViewModel @Inject constructor(
    val sessionPreferences: SessionPreferences,
) : ViewModel()
