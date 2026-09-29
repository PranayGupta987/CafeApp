package com.cafeapp.feature.reservations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafeapp.core.network.dto.ReservationDto
import com.cafeapp.core.ui.components.CafeButton
import com.cafeapp.core.ui.components.CafeButtonVariant
import com.cafeapp.core.ui.components.CafeEmptyState
import com.cafeapp.core.ui.components.CafeErrorState
import com.cafeapp.core.ui.components.CafeLoadingIndicator
import com.cafeapp.core.ui.components.CafeTopAppBar
import com.cafeapp.core.ui.components.StatusBadge
import com.cafeapp.core.ui.theme.Dimens
import com.cafeapp.core.ui.theme.StatusError
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerReservationsScreen(
    viewModel: CustomerReservationsViewModel = hiltViewModel(),
    onNavigateBack: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            CafeTopAppBar(
                title = "Table Reservations",
                subtitle = "Reserve your dining spot",
                onNavigateBack = onNavigateBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.space16)
            ) {
                CafeButton(
                    text = "Book a Table",
                    onClick = { viewModel.openBookingDialog() },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.EventSeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(Dimens.buttonIconSize),
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CafeLoadingIndicator(message = "Checking table bookings...")
                }
                uiState.reservations.isEmpty() -> {
                    CafeEmptyState(
                        title = "No reservations yet",
                        description = "Reserve a table in advance for a relaxed dine-in experience.",
                        actionLabel = "Reserve Now",
                        onAction = { viewModel.openBookingDialog() }
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = Dimens.space16,
                            end = Dimens.space16,
                            top = Dimens.space16,
                            bottom = 90.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(Dimens.space12),
                    ) {
                        items(
                            items = uiState.reservations,
                            key = { it.id }
                        ) { reservation ->
                            ReservationCard(
                                reservation = reservation,
                                onCancel = { viewModel.cancelReservation(reservation.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isBookingDialogOpen) {
        BookingDialog(
            partySize = uiState.partySize,
            selectedDateTime = uiState.selectedDateTime,
            durationMinutes = uiState.durationMinutes,
            specialRequests = uiState.specialRequests,
            isSubmitting = uiState.isSubmitting,
            onPartySizeChange = { viewModel.setPartySize(it) },
            onDateTimeChange = { viewModel.setDateTime(it) },
            onDurationChange = { viewModel.setDuration(it) },
            onSpecialRequestsChange = { viewModel.setSpecialRequests(it) },
            onSubmit = { viewModel.submitReservation() },
            onDismiss = { viewModel.closeBookingDialog() }
        )
    }
}

@Composable
fun ReservationCard(
    reservation: ReservationDto,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusMedium),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.space16)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EventSeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Dimens.space8))
                    val resTable = reservation.table
                    Text(
                        text = if (resTable != null) "Table ${resTable.tableNumber}" else "Table Pending",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                StatusBadge(status = reservation.status)
            }

            Spacer(modifier = Modifier.height(Dimens.space12))

            // Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.space16),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(Dimens.space4))
                    Text(
                        text = "${reservation.partySize} Guests",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(Dimens.space4))
                    Text(
                        text = "${reservation.durationMinutes} mins",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.space8))

            // Formatted Date
            val displayTime = formatReservationTime(reservation.reservationTime)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(Dimens.space4))
                Text(
                    text = displayTime,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            val specialReq = reservation.specialRequests
            if (!specialReq.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Dimens.space8))
                Text(
                    text = "Note: $specialReq",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Cancel action if eligible
            val isCancellable = reservation.status.uppercase() in listOf("PENDING", "CONFIRMED")
            if (isCancellable) {
                Spacer(modifier = Modifier.height(Dimens.space12))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(contentColor = StatusError)
                    ) {
                        Text("Cancel Reservation")
                    }
                }
            }
        }
    }
}

@Composable
fun BookingDialog(
    partySize: Int,
    selectedDateTime: String,
    durationMinutes: Int,
    specialRequests: String,
    isSubmitting: Boolean,
    onPartySizeChange: (Int) -> Unit,
    onDateTimeChange: (String) -> Unit,
    onDurationChange: (Int) -> Unit,
    onSpecialRequestsChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Book a Table",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.space16)
            ) {
                // Party Size
                Column {
                    Text(
                        text = "Number of Guests: $partySize",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.height(Dimens.space8))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                    ) {
                        listOf(1, 2, 3, 4, 5, 6, 8, 10).forEach { size ->
                            FilterChip(
                                selected = partySize == size,
                                onClick = { onPartySizeChange(size) },
                                label = { Text("$size") },
                            )
                        }
                    }
                }

                // Reservation Time presets
                Column {
                    Text(
                        text = "Select Time Slot",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.height(Dimens.space8))

                    val timeSlots = remember { generateTimeSlots() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                    ) {
                        timeSlots.forEach { (label, iso) ->
                            FilterChip(
                                selected = selectedDateTime == iso,
                                onClick = { onDateTimeChange(iso) },
                                label = { Text(label, fontSize = 12.sp) },
                            )
                        }
                    }
                }

                // Duration
                Column {
                    Text(
                        text = "Duration: $durationMinutes minutes",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(modifier = Modifier.height(Dimens.space8))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                    ) {
                        listOf(60, 90, 120).forEach { dur ->
                            FilterChip(
                                selected = durationMinutes == dur,
                                onClick = { onDurationChange(dur) },
                                label = { Text("$dur min") },
                            )
                        }
                    }
                }

                // Special Requests
                OutlinedTextField(
                    value = specialRequests,
                    onValueChange = onSpecialRequestsChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Special Requests (optional)") },
                    placeholder = { Text("e.g., Quiet corner, High chair, Birthday celebration") },
                    maxLines = 3,
                    shape = RoundedCornerShape(Dimens.radiusMedium),
                )
            }
        },
        confirmButton = {
            CafeButton(
                text = "Confirm Booking",
                onClick = onSubmit,
                isLoading = isSubmitting,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatReservationTime(isoString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val date = inputFormat.parse(isoString.take(19)) ?: return isoString
        val outputFormat = SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        isoString
    }
}

private fun generateTimeSlots(): List<Pair<String, String>> {
    val slots = mutableListOf<Pair<String, String>>()
    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
    val displayFormat = SimpleDateFormat("EEE, hh:mm a", Locale.getDefault())

    val cal = Calendar.getInstance()
    cal.add(Calendar.HOUR_OF_DAY, 1)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)

    for (i in 0..4) {
        slots.add(Pair(displayFormat.format(cal.time), isoFormat.format(cal.time)))
        cal.add(Calendar.HOUR_OF_DAY, 2)
    }
    return slots
}
