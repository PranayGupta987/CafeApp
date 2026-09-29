package com.cafeapp.feature.reservations

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafeapp.core.network.dto.AvailableTableDto
import com.cafeapp.core.network.dto.ReservationDto
import com.cafeapp.core.network.dto.TimeSlotDto
import com.cafeapp.core.ui.components.CafeButton
import com.cafeapp.core.ui.components.CafeButtonVariant
import com.cafeapp.core.ui.components.CafeEmptyState
import com.cafeapp.core.ui.components.CafeErrorState
import com.cafeapp.core.ui.components.CafeLoadingIndicator
import com.cafeapp.core.ui.components.CafeTopAppBar
import com.cafeapp.core.ui.components.StatusBadge
import com.cafeapp.core.ui.theme.CafeBrown
import com.cafeapp.core.ui.theme.CafeGold
import com.cafeapp.core.ui.theme.Dimens
import com.cafeapp.core.ui.theme.StatusError
import com.cafeapp.core.ui.theme.StatusSuccess
import java.text.SimpleDateFormat
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
                subtitle = "Book and manage your dining experience",
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
                    text = "Book a New Table",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs: Upcoming vs Past
            TabRow(
                selectedTabIndex = uiState.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab.ordinal]),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            ) {
                Tab(
                    selected = uiState.selectedTab == ReservationTab.UPCOMING,
                    onClick = { viewModel.selectTab(ReservationTab.UPCOMING) },
                    text = {
                        Text(
                            text = "Upcoming (${uiState.upcomingReservations.size})",
                            fontWeight = if (uiState.selectedTab == ReservationTab.UPCOMING) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = uiState.selectedTab == ReservationTab.PAST,
                    onClick = { viewModel.selectTab(ReservationTab.PAST) },
                    text = {
                        Text(
                            text = "Past (${uiState.pastReservations.size})",
                            fontWeight = if (uiState.selectedTab == ReservationTab.PAST) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (uiState.isLoading && uiState.reservations.isEmpty()) {
                    CafeLoadingIndicator(message = "Loading your table reservations...")
                } else {
                    val currentList = if (uiState.selectedTab == ReservationTab.UPCOMING) {
                        uiState.upcomingReservations
                    } else {
                        uiState.pastReservations
                    }

                    if (currentList.isEmpty()) {
                        if (uiState.selectedTab == ReservationTab.UPCOMING) {
                            CafeEmptyState(
                                title = "No Upcoming Reservations",
                                description = "Reserve a table in advance to skip the queue and guarantee your spot.",
                                actionLabel = "Book a Table",
                                onAction = { viewModel.openBookingDialog() }
                            )
                        } else {
                            CafeEmptyState(
                                title = "No Past Bookings",
                                description = "You haven't completed or cancelled any reservations yet.",
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = Dimens.space16,
                                end = Dimens.space16,
                                top = Dimens.space16,
                                bottom = 80.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(Dimens.space12),
                        ) {
                            items(
                                items = currentList,
                                key = { it.id }
                            ) { reservation ->
                                ReservationCard(
                                    reservation = reservation,
                                    onClick = { viewModel.showReservationDetails(reservation) },
                                    onCancel = { viewModel.requestCancelReservation(reservation) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Reservation Details Modal Sheet
    if (uiState.selectedReservationForDetails != null) {
        ReservationDetailsDialog(
            reservation = uiState.selectedReservationForDetails!!,
            onDismiss = { viewModel.dismissReservationDetails() },
            onCancel = {
                val res = uiState.selectedReservationForDetails!!
                viewModel.dismissReservationDetails()
                viewModel.requestCancelReservation(res)
            }
        )
    }

    // Cancellation Confirmation Alert Dialog
    if (uiState.reservationToCancel != null) {
        val res = uiState.reservationToCancel!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissCancelConfirmation() },
            title = {
                Text(
                    text = "Cancel Table Reservation?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to cancel your table reservation for ${res.partySize} guests on ${formatReservationTime(res.reservationTime)}?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Dimens.space8))
                    Text(
                        text = "This action will release the table and cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                CafeButton(
                    text = "Yes, Cancel Reservation",
                    onClick = { viewModel.confirmCancelReservation() },
                    variant = CafeButtonVariant.DESTRUCTIVE,
                    isLoading = uiState.isCancelling,
                )
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCancelConfirmation() }) {
                    Text("Keep Booking")
                }
            }
        )
    }

    // Comprehensive Booking Modal Sheet / Dialog
    if (uiState.isBookingDialogOpen) {
        BookingModalSheet(
            uiState = uiState,
            viewModel = viewModel,
        )
    }
}

@Composable
fun ReservationCard(
    reservation: ReservationDto,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCancellable = reservation.status.uppercase() in listOf("PENDING", "CONFIRMED")
    val resTable = reservation.table

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
            // Header Row: Table badge & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventSeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Dimens.space8))
                    Column {
                        Text(
                            text = if (resTable != null) "Table #${resTable.tableNumber}" else "Table (Auto-Assigned)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (resTable != null) {
                            Text(
                                text = "Capacity: ${resTable.capacity} guests",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                StatusBadge(status = reservation.status)
            }

            Spacer(modifier = Modifier.height(Dimens.space12))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(Dimens.space12))

            // Date & Time
            val displayTime = formatReservationTime(reservation.reservationTime)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(Dimens.space6))
                Text(
                    text = displayTime,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.space8))

            // Details Row: Guests & Duration
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
                        text = "${reservation.durationMinutes} mins slot",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            val specialReq = reservation.specialRequests
            if (!specialReq.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Dimens.space8))
                Text(
                    text = "“$specialReq”",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Actions: Details & Cancel
            Spacer(modifier = Modifier.height(Dimens.space12))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to view details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (isCancellable) {
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(contentColor = StatusError)
                    ) {
                        Text("Cancel Booking", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingModalSheet(
    uiState: ReservationsUiState,
    viewModel: CustomerReservationsViewModel,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { viewModel.closeBookingDialog() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = Dimens.radiusLarge, topEnd = Dimens.radiusLarge),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.space16, vertical = Dimens.space8)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (uiState.isConfirmationStep) "Confirm Reservation" else "Book a Table",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (uiState.isConfirmationStep) "Review your booking details" else "Choose date, party size & dining slot",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { viewModel.closeBookingDialog() }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(Dimens.space16))

            if (!uiState.isConfirmationStep) {
                // STEP 1: SELECTION CONTROLS

                // 1. DATE PICKER HORIZONTAL STRIP
                Text(
                    text = "1. Select Date",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Dimens.space8))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                ) {
                    items(uiState.dateOptions) { option ->
                        val isSelected = uiState.selectedDate == option.dateStr
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setDate(option.dateStr) },
                            label = { Text(option.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.space16))

                // 2. GUESTS COUNT PICKER
                Text(
                    text = "2. Number of Guests",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Dimens.space8))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                ) {
                    listOf(1, 2, 3, 4, 5, 6, 8, 10).forEach { size ->
                        val isSelected = uiState.partySize == size
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setPartySize(size) },
                            label = { Text("$size ${if (size == 1) "Guest" else "Guests"}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.space16))

                // 3. TIME SLOT SELECTION
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. Select Time Slot",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (uiState.isCheckingAvailability) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    }
                }
                Spacer(modifier = Modifier.height(Dimens.space8))

                val slots = uiState.availabilityData?.timeSlots ?: emptyList()
                if (slots.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                    ) {
                        items(slots) { slot ->
                            val isSelected = uiState.selectedTime == slot.time
                            val isEnabled = slot.isAvailable && slot.availableTablesCount > 0

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isEnabled) {
                                        viewModel.setTimeSlot(slot.time, slot.isoString)
                                    }
                                },
                                enabled = isEnabled,
                                label = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = slot.time,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = if (isEnabled) "${slot.availableTablesCount} left" else "Full",
                                            fontSize = 9.sp,
                                            color = if (isEnabled) (if (isSelected) MaterialTheme.colorScheme.primary else StatusSuccess) else StatusError
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            )
                        }
                    }
                } else {
                    // Fallback slots if network pending
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                    ) {
                        listOf("11:00", "12:00", "13:00", "14:00", "17:00", "18:00", "19:00", "20:00").forEach { time ->
                            val isSelected = uiState.selectedTime == time
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setTimeSlot(time, "") },
                                label = { Text(time) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.space16))

                // 4. TABLE PREFERENCE / AUTO-ASSIGN
                Text(
                    text = "4. Table Selection",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Dimens.space8))

                val availableTables = uiState.availabilityData?.availableTables ?: emptyList()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                ) {
                    // Auto-assign option
                    val isAutoSelected = uiState.selectedTableId == null
                    FilterChip(
                        selected = isAutoSelected,
                        onClick = { viewModel.selectTable(null) },
                        label = { Text("✨ Auto-Assign Best Table") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    )

                    // Individual available tables
                    availableTables.forEach { table ->
                        val isTableSelected = uiState.selectedTableId == table.id
                        FilterChip(
                            selected = isTableSelected,
                            onClick = { viewModel.selectTable(table) },
                            label = { Text("Table #${table.tableNumber} (${table.capacity} seats)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.space16))

                // 5. DURATION
                Text(
                    text = "5. Dining Duration",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(Dimens.space8))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space8)
                ) {
                    listOf(60, 90, 120).forEach { dur ->
                        FilterChip(
                            selected = uiState.durationMinutes == dur,
                            onClick = { viewModel.setDuration(dur) },
                            label = { Text("$dur minutes") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.space16))

                // 6. SPECIAL REQUESTS
                OutlinedTextField(
                    value = uiState.specialRequests,
                    onValueChange = { viewModel.setSpecialRequests(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Special Requests / Notes (Optional)") },
                    placeholder = { Text("e.g. Birthday celebration, Quiet booth, Baby high-chair") },
                    maxLines = 3,
                    shape = RoundedCornerShape(Dimens.radiusMedium),
                )

                Spacer(modifier = Modifier.height(Dimens.space24))

                // CTA Button
                CafeButton(
                    text = "Review Booking Details",
                    onClick = { viewModel.proceedToConfirmation() },
                    modifier = Modifier.fillMaxWidth(),
                )

            } else {
                // STEP 2: CONFIRMATION & REVIEW SUMMARY
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.radiusMedium),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.space16)
                    ) {
                        Text(
                            text = "Reservation Summary",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(Dimens.space12))

                        SummaryItem(
                            icon = Icons.Default.CalendarToday,
                            label = "Date & Time",
                            value = "${uiState.selectedDate} at ${uiState.selectedTime}"
                        )
                        SummaryItem(
                            icon = Icons.Default.Group,
                            label = "Party Size",
                            value = "${uiState.partySize} Guests"
                        )
                        SummaryItem(
                            icon = Icons.Default.TableBar,
                            label = "Table Assignment",
                            value = if (uiState.selectedTableNumber != null) "Table #${uiState.selectedTableNumber}" else "Auto-Assigned Table"
                        )
                        SummaryItem(
                            icon = Icons.Default.Schedule,
                            label = "Duration",
                            value = "${uiState.durationMinutes} Minutes"
                        )
                        if (uiState.specialRequests.isNotBlank()) {
                            SummaryItem(
                                icon = Icons.Default.Info,
                                label = "Special Requests",
                                value = uiState.specialRequests
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.space16))

                // Terms / Notice Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.radiusSmall),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(Dimens.space12),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Dimens.space8))
                        Text(
                            text = "Please arrive within 15 minutes of your booking time. Tables will be held for up to 15 minutes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.space24))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.space12)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.backToSelection() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(Dimens.radiusMedium),
                    ) {
                        Text("Back")
                    }

                    CafeButton(
                        text = "Confirm Booking",
                        onClick = { viewModel.submitReservation() },
                        modifier = Modifier.weight(2f),
                        isLoading = uiState.isSubmitting,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.space32))
        }
    }
}

@Composable
fun SummaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.space4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(Dimens.space8))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ReservationDetailsDialog(
    reservation: ReservationDto,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
) {
    val isCancellable = reservation.status.uppercase() in listOf("PENDING", "CONFIRMED")
    val resTable = reservation.table

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reservation Details",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                StatusBadge(status = reservation.status)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.space12)
            ) {
                // Table and Time Hero
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimens.radiusMedium),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.space16),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (resTable != null) "Table #${resTable.tableNumber}" else "Table Reserved",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(Dimens.space4))
                        Text(
                            text = formatReservationTime(reservation.reservationTime),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }

                // Booking Info
                SummaryItem(
                    icon = Icons.Default.Group,
                    label = "Guests",
                    value = "${reservation.partySize} Persons"
                )

                SummaryItem(
                    icon = Icons.Default.Schedule,
                    label = "Duration",
                    value = "${reservation.durationMinutes} Minutes"
                )

                if (resTable != null) {
                    SummaryItem(
                        icon = Icons.Default.TableBar,
                        label = "Table Capacity",
                        value = "${resTable.capacity} Seats"
                    )
                }

                val notes = reservation.specialRequests
                if (!notes.isNullOrBlank()) {
                    SummaryItem(
                        icon = Icons.Default.Info,
                        label = "Special Requests",
                        value = notes
                    )
                }

                SummaryItem(
                    icon = Icons.Default.Info,
                    label = "Booking ID",
                    value = reservation.id.take(12) + "..."
                )
            }
        },
        confirmButton = {
            if (isCancellable) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError)
                ) {
                    Text("Cancel Reservation")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            if (isCancellable) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}

private fun formatReservationTime(isoString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val cleanIso = if (isoString.length >= 19) isoString.substring(0, 19) else isoString
        val date = inputFormat.parse(cleanIso) ?: return isoString
        val outputFormat = SimpleDateFormat("EEE, dd MMM yyyy • hh:mm a", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        isoString
    }
}
