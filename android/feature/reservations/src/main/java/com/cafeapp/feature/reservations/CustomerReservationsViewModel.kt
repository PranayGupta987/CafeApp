package com.cafeapp.feature.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeapp.core.network.ReservationsApi
import com.cafeapp.core.network.dto.CreateReservationRequest
import com.cafeapp.core.network.dto.ReservationDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ReservationsUiState(
    val isLoading: Boolean = false,
    val reservations: List<ReservationDto> = emptyList(),
    val isBookingDialogOpen: Boolean = false,
    val isSubmitting: Boolean = false,
    val partySize: Int = 2,
    val selectedDateTime: String = "",
    val durationMinutes: Int = 90,
    val specialRequests: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class CustomerReservationsViewModel @Inject constructor(
    private val reservationsApi: ReservationsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReservationsUiState(isLoading = true))
    val uiState: StateFlow<ReservationsUiState> = _uiState.asStateFlow()

    init {
        setDefaultReservationTime()
        loadReservations()
    }

    private fun setDefaultReservationTime() {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        _uiState.update { it.copy(selectedDateTime = isoFormat.format(calendar.time)) }
    }

    fun loadReservations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val response = reservationsApi.getReservations()
                val reservations = response.data ?: emptyList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        reservations = reservations,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to load reservations"
                    )
                }
            }
        }
    }

    fun openBookingDialog() {
        setDefaultReservationTime()
        _uiState.update {
            it.copy(
                isBookingDialogOpen = true,
                partySize = 2,
                durationMinutes = 90,
                specialRequests = "",
            )
        }
    }

    fun closeBookingDialog() {
        _uiState.update { it.copy(isBookingDialogOpen = false) }
    }

    fun setPartySize(size: Int) {
        _uiState.update { it.copy(partySize = size) }
    }

    fun setDateTime(isoString: String) {
        _uiState.update { it.copy(selectedDateTime = isoString) }
    }

    fun setDuration(minutes: Int) {
        _uiState.update { it.copy(durationMinutes = minutes) }
    }

    fun setSpecialRequests(notes: String) {
        _uiState.update { it.copy(specialRequests = notes) }
    }

    fun submitReservation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val request = CreateReservationRequest(
                    partySize = _uiState.value.partySize,
                    reservationTime = _uiState.value.selectedDateTime,
                    durationMinutes = _uiState.value.durationMinutes,
                    specialRequests = _uiState.value.specialRequests.ifBlank { null }
                )
                val response = reservationsApi.createReservation(request)
                if (response.success) {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isBookingDialogOpen = false,
                            successMessage = "Table reserved successfully!",
                        )
                    }
                    loadReservations()
                } else {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = response.message ?: "Booking failed"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.localizedMessage ?: "Failed to reserve table"
                    )
                }
            }
        }
    }

    fun cancelReservation(id: String) {
        viewModelScope.launch {
            try {
                reservationsApi.cancelReservation(id)
                _uiState.update { it.copy(successMessage = "Reservation cancelled") }
                loadReservations()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = e.localizedMessage ?: "Failed to cancel reservation")
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
