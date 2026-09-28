package com.cafeapp.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeapp.core.datastore.SessionPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionPreferences: SessionPreferences,
) : ViewModel() {

    sealed interface Destination {
        data object Login : Destination
        data object Customer : Destination
        data object Cafe : Destination
        data object Kitchen : Destination
    }

    private val _destination = MutableSharedFlow<Destination>(replay = 0)
    val destination: SharedFlow<Destination> = _destination

    init {
        viewModelScope.launch {
            delay(800) // Brief splash display
            val token = sessionPreferences.accessToken.first()
            val role = sessionPreferences.userRole.first()
            val dest = if (token.isNullOrBlank()) {
                Destination.Login
            } else {
                when (role) {
                    "CAFE_ADMIN", "CAFE_STAFF" -> Destination.Cafe
                    "KITCHEN" -> Destination.Kitchen
                    else -> Destination.Customer
                }
            }
            _destination.emit(dest)
        }
    }
}
