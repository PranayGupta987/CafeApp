package com.cafeapp.feature.kitchen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.cafeapp.core.network.dto.OrderDto
import com.cafeapp.core.ui.CurrencyFormatter
import com.cafeapp.core.ui.components.CafeEmptyState
import com.cafeapp.core.ui.components.CafeErrorState
import com.cafeapp.core.ui.components.CafeLoadingIndicator
import com.cafeapp.core.ui.components.StatusBadge
import com.cafeapp.core.ui.components.VegIndicator
import com.cafeapp.core.ui.theme.Dimens
import com.cafeapp.core.ui.theme.CafeBrownDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenScreen(
    viewModel: KitchenViewModel = hiltViewModel(),
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    val activeCount = uiState.orders.count {
        it.status.uppercase() in listOf("PLACED", "ACCEPTED", "PREPARING", "READY")
    }
    val placedCount = uiState.orders.count { it.status.uppercase() in listOf("PLACED", "ACCEPTED") }
    val preparingCount = uiState.orders.count { it.status.uppercase() == "PREPARING" }
    val readyCount = uiState.orders.count { it.status.uppercase() == "READY" }

    val filteredOrders = when (uiState.activeTab) {
        "ACTIVE" -> uiState.orders.filter {
            it.status.uppercase() in listOf("PLACED", "ACCEPTED", "PREPARING", "READY")
        }
        "PLACED" -> uiState.orders.filter { it.status.uppercase() in listOf("PLACED", "ACCEPTED") }
        "PREPARING" -> uiState.orders.filter { it.status.uppercase() == "PREPARING" }
        "READY" -> uiState.orders.filter { it.status.uppercase() == "READY" }
        "SERVED" -> uiState.orders.filter { it.status.uppercase() == "SERVED" }
        else -> uiState.orders
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Kitchen Display (KDS)",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(Dimens.space8))
                        // Real-time status dot
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (uiState.isSocketConnected) Color(0xFF4CAF50) else Color(0xFFFF9800))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (uiState.isSocketConnected) "LIVE" else "OFFLINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = Color.White,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CafeBrownDark,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadOrders() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row with Badge Counters
            val tabs = listOf(
                Triple("ACTIVE", "Active Queue", activeCount),
                Triple("PLACED", "New Orders", placedCount),
                Triple("PREPARING", "Preparing", preparingCount),
                Triple("READY", "Ready", readyCount),
                Triple("SERVED", "Served", null),
                Triple("ALL", "All History", null),
            )
            val selectedTabIndex = tabs.indexOfFirst { it.first == uiState.activeTab }.coerceAtLeast(0)

            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = Dimens.space12,
            ) {
                tabs.forEachIndexed { index, (key, label, count) ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { viewModel.setActiveTab(key) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                                if (count != null && count > 0) {
                                    Spacer(modifier = Modifier.width(Dimens.space6))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (selectedTabIndex == index) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$count",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (selectedTabIndex == index) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        CafeLoadingIndicator(message = "Loading Kitchen tickets...")
                    }
                    uiState.errorMessage != null && uiState.orders.isEmpty() -> {
                        CafeErrorState(
                            message = uiState.errorMessage ?: "Failed to fetch kitchen orders",
                            onRetry = { viewModel.loadOrders() }
                        )
                    }
                    filteredOrders.isEmpty() -> {
                        CafeEmptyState(
                            title = "Kitchen Queue Clear",
                            description = if (uiState.activeTab == "ACTIVE") "No tickets in active cooking queue right now." else "No orders match this filter.",
                            actionLabel = "Refresh Queue",
                            onAction = { viewModel.loadOrders() }
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(Dimens.space16),
                            verticalArrangement = Arrangement.spacedBy(Dimens.space16)
                        ) {
                            items(
                                items = filteredOrders,
                                key = { it.id }
                            ) { order ->
                                KitchenOrderCard(
                                    order = order,
                                    isUpdating = uiState.updatingOrderId == order.id,
                                    onUpdateStatus = { nextStatus ->
                                        viewModel.updateStatus(order.id, nextStatus)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KitchenOrderCard(
    order: OrderDto,
    isUpdating: Boolean,
    onUpdateStatus: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusUpper = order.status.uppercase()
    val isUrgent = statusUpper == "PLACED" || statusUpper == "ACCEPTED"

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusMedium),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isUrgent) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isUrgent) 4.dp else 2.dp
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.space16)
        ) {
            // Header: Table Number & Order Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Prominent Table Badge
                    val tableNumber = order.table?.tableNumber
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Dimens.radiusSmall))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = Dimens.space8, vertical = Dimens.space4)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TableBar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(Dimens.space4))
                            Text(
                                text = if (tableNumber != null) "Table #$tableNumber" else "Dine-in",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(Dimens.space8))

                    Text(
                        text = "Order #${order.orderNumber}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                StatusBadge(status = order.status)
            }

            Spacer(modifier = Modifier.height(Dimens.space8))

            // Time and items count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(Dimens.space4))
                    Text(
                        text = formatElapsedOrTime(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = "${order.items.sumOf { it.quantity }} items",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.space12))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Dimens.space12))

            // Items List
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.space8)) {
                order.items.forEach { item ->
                    val menuItem = item.menuItem
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(Dimens.radiusSmall))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = Dimens.space6, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${item.quantity}x",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }

                            Spacer(modifier = Modifier.width(Dimens.space8))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    VegIndicator(isVeg = menuItem?.isVeg ?: true, size = 10.dp)
                                    Spacer(modifier = Modifier.width(Dimens.space6))
                                    Text(
                                        text = menuItem?.name ?: "Item",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                if (!item.notes.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Note: ${item.notes}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Special instructions banner
            if (!order.specialInstructions.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Dimens.space12))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.radiusSmall))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(Dimens.space8)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(Dimens.space6))
                        Text(
                            text = "Special Note: ${order.specialInstructions}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.space16))

            // Action Buttons based on status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    when (statusUpper) {
                        "PLACED", "ACCEPTED" -> {
                            Button(
                                onClick = { onUpdateStatus("PREPARING") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(Dimens.radiusMedium)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OutdoorGrill,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(Dimens.space6))
                                Text("Start Preparing")
                            }
                        }
                        "PREPARING" -> {
                            Button(
                                onClick = { onUpdateStatus("READY") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), // Dark Green
                                shape = RoundedCornerShape(Dimens.radiusMedium)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(Dimens.space6))
                                Text("Mark Ready")
                            }
                        }
                        "READY" -> {
                            Button(
                                onClick = { onUpdateStatus("SERVED") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)), // Blue
                                shape = RoundedCornerShape(Dimens.radiusMedium)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(Dimens.space6))
                                Text("Mark Served")
                            }
                        }
                        "SERVED" -> {
                            OutlinedButton(
                                onClick = { onUpdateStatus("COMPLETED") },
                                shape = RoundedCornerShape(Dimens.radiusMedium)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(Dimens.space6))
                                Text("Complete Order")
                            }
                        }
                        "COMPLETED" -> {
                            Text(
                                text = "Order Fulfilled",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatElapsedOrTime(isoString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val date = inputFormat.parse(isoString.take(19)) ?: return isoString
        val diffMillis = System.currentTimeMillis() - date.time
        val minutes = (diffMillis / (1000 * 60)).toInt()

        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            else -> {
                val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                outputFormat.format(date)
            }
        }
    } catch (e: Exception) {
        isoString
    }
}
