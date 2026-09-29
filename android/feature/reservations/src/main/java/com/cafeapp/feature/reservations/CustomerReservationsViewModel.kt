package com.cafeapp.feature.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cafeapp.core.network.ReservationsApi
import com.cafeapp.core.network.dto.AvailabilityDataDto
import com.cafeapp.core.network.dto.AvailableTableDto
import com.cafeapp.core.network.dto.CreateReservationRequest
import com.cafeapp.core.network.dto.ReservationDto
import com.cafeapp.core.network.dto.TimeSlotDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

enum class ReservationTab {
    UPCOMING,
    PAST
}

data class DateOption(
    val label: String,      // e.g. "Today", "Tomorrow", "Thu, 12 Oct"
    val dateStr: String,    // "YYYY-MM-DD"
    val isToday: Boolean = false,
)

data class ReservationsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedTab: ReservationTab = ReservationTab.UPCOMING,
    val reservations: List<ReservationDto> = emptyList(),

    // Booking Dialog / Sheet State
    val isBookingDialogOpen: Boolean = false,
    val isSubmitting: Boolean = false,
    val isCheckingAvailability: Boolean = false,

    val dateOptions: List<DateOption> = emptyList(),
    val selectedDate: String = "",
    val selectedTime: String = "12:00",
    val selectedTimeSlotIso: String = "",
    val partySize: Int = 2,
    val durationMinutes: Int = 90,
    val selectedTableId: String? = null,
    val selectedTableNumber: Int? = null,
    val specialRequests: String = "",

    val availabilityData: AvailabilityDataDto? = null,
    val isConfirmationStep: Boolean = false,

    // Dialogs / Sheets
    val selectedReservationForDetails: ReservationDto? = null,
    val reservationToCancel: ReservationDto? = null,
    val isCancelling: Boolean = false,

    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val upcomingReservations: List<ReservationDto>
        get() = reservations.filter {
            val status = it.status.uppercase()
            status in listOf("PENDING", "CONFIRMED", "SEATED")
        }

    val pastReservations: List<ReservationDto>
        get() = reservations.filter {
            val status = it.status.uppercase()
            status in listOf("COMPLETED", "CANCELLED", "NO_SHOW", "REJECTED")
        }
}

@HiltViewModel
class CustomerReservationsViewModel @Inject constructor(
    private val reservationsApi: ReservationsApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReservationsUiState(isLoading = true))
    val uiState: StateFlow<ReservationsUiState> = _uiState.asStateFlow()

    private var availabilityJob: Job? = null

    init {
        initializeDateOptions()
        loadReservations()
    }

    private fun initializeDateOptions() {
        val options = mutableListOf<DateOption>()
        val cal = Calendar.getInstance()
        val ymdFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val displayFormat = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())

        for (i in 0..13) {
            val date = cal.time
            val dateStr = ymdFormat.format(date)
            val label = when (i) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> displayFormat.format(date)
            }
            options.add(DateOption(label = label, dateStr = dateStr, isToday = i == 0))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val initialDate = options.firstOrNull()?.dateStr ?: ymdFormat.format(Date())
        _uiState.update {
            it.copy(
                dateOptions = options,
                selectedDate = initialDate,
            )
        }
    }

    fun selectTab(tab: ReservationTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun loadReservations(isRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true) else it.copy(isLoading = true, errorMessage = null)
            }
            try {
                val response = reservationsApi.getReservations()
                val reservations = response.data ?: emptyList()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        reservations = reservations,
                        errorMessage = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = e.localizedMessage ?: "Failed to load reservations"
                    )
                }
            }
        }
    }

    fun openBookingDialog() {
        val todayStr = _uiState.value.dateOptions.firstOrNull()?.dateStr ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val nextSlotTime = calculateDefaultSlotTime()

        _uiState.update {
            it.copy(
                isBookingDialogOpen = true,
                isConfirmationStep = false,
                selectedDate = todayStr,
                selectedTime = nextSlotTime,
                selectedTimeSlotIso = "",
                partySize = 2,
                durationMinutes = 90,
                selectedTableId = null,
                selectedTableNumber = null,
                specialRequests = "",
            )
        }
        fetchAvailability()
    }

    private fun calculateDefaultSlotTime(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.HOUR_OF_DAY, 2)
        val hour = cal.get(Calendar.HOUR_OF_DAY).coerceIn(10, 21)
        val minute = if (cal.get(Calendar.MINUTE) < 30) "00" else "30"
        return "${hour.toString().padStart(2, '0')}:$minute"
    }

    fun closeBookingDialog() {
        _uiState.update {
            it.copy(
                isBookingDialogOpen = false,
                isConfirmationStep = false,
                selectedReservationForDetails = null,
            )
        }
    }

    fun setDate(dateStr: String) {
        _uiState.update { it.copy(selectedDate = dateStr, selectedTableId = null, selectedTableNumber = null) }
        fetchAvailability()
    }

    fun setPartySize(size: Int) {
        _uiState.update { it.copy(partySize = size, selectedTableId = null, selectedTableNumber = null) }
        fetchAvailability()
    }

    fun setTimeSlot(timeStr: String, isoString: String) {
        _uiState.update {
            it.copy(
                selectedTime = timeStr,
                selectedTimeSlotIso = isoString,
                selectedTableId = null,
                selectedTableNumber = null,
            )
        }
        fetchAvailability()
    }

    fun setDuration(minutes: Int) {
        _uiState.update { it.copy(durationMinutes = minutes) }
        fetchAvailability()
    }

    fun selectTable(table: AvailableTableDto?) {
        _uiState.update {
            it.copy(
                selectedTableId = table?.id,
                selectedTableNumber = table?.tableNumber,
            )
        }
    }

    fun setSpecialRequests(notes: String) {
        _uiState.update { it.copy(specialRequests = notes) }
    }

    fun proceedToConfirmation() {
        _uiState.update { it.copy(isConfirmationStep = true) }
    }

    fun backToSelection() {
        _uiState.update { it.copy(isConfirmationStep = false) }
    }

    fun fetchAvailability() {
        availabilityJob?.cancel()
        availabilityJob = viewModelScope.launch {
            _uiState.update { it.copy(isCheckingAvailability = true) }
            try {
                val state = _uiState.value
                val response = reservationsApi.getAvailability(
                    date = state.selectedDate,
                    time = state.selectedTime,
                    partySize = state.partySize,
                    durationMinutes = state.durationMinutes,
                )

                val data = response.data
                if (response.success && data != null) {
                    val matchedSlot = data.timeSlots.find { it.time == state.selectedTime }
                    val isoString = matchedSlot?.isoString ?: data.requestedTime ?: "${state.selectedDate}T${state.selectedTime}:00Z"

                    _uiState.update {
                        it.copy(
                            isCheckingAvailability = false,
                            availabilityData = data,
                            selectedTimeSlotIso = isoString,
                        )
                    }
                } else {
                    _uiState.update { it.copy(isCheckingAvailability = false) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCheckingAvailability = false,
                    )
                }
            }
        }
    }

    fun submitReservation() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }

            // Construct valid ISO-8601 reservation time string
            var resTimeIso = state.selectedTimeSlotIso
            if (resTimeIso.isBlank()) {
                resTimeIso = "${state.selectedDate}T${state.selectedTime}:00.000Z"
            }

            try {
                val request = CreateReservationRequest(
                    partySize = state.partySize,
                    reservationTime = resTimeIso,
                    tableId = state.selectedTableId,
                    durationMinutes = state.durationMinutes,
                    specialRequests = state.specialRequests.ifBlank { null },
                )
                val response = reservationsApi.createReservation(request)
                if (response.success) {
                    val tableDesc = if (state.selectedTableNumber != null) "at Table #${state.selectedTableNumber}" else ""
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isBookingDialogOpen = false,
                            isConfirmationStep = false,
                            successMessage = "Table reserved successfully $tableDesc!",
                        )
                    }
                    loadReservations()
                } else {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = response.message ?: "Failed to complete reservation"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = e.localizedMessage ?: "Failed to reserve table. Please try another slot."
                    )
                }
            }
        }
    }

    fun showReservationDetails(reservation: ReservationDto) {
        _uiState.update { it.copy(selectedReservationForDetails = reservation) }
    }

    fun dismissReservationDetails() {
        _uiState.update { it.copy(selectedReservationForDetails = null) }
    }

    fun requestCancelReservation(reservation: ReservationDto) {
        _uiState.update { it.copy(reservationToCancel = reservation) }
    }

    fun dismissCancelConfirmation() {
        _uiState.update { it.copy(reservationToCancel = null) }
    }

    fun confirmCancelReservation() {
        val reservation = _uiState.value.reservationToCancel ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true) }
            try {
                val response = reservationsApi.cancelReservation(reservation.id)
                _uiState.update {
                    it.copy(
                        isCancelling = false,
                        reservationToCancel = null,
                        selectedReservationForDetails = null,
                        successMessage = "Reservation cancelled successfully",
                    )
                }
                loadReservations()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isCancelling = false,
                        errorMessage = e.localizedMessage ?: "Failed to cancel reservation"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
