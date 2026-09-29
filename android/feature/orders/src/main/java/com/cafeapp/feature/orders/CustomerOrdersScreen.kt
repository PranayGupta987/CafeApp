package com.cafeapp.feature.orders

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.cafeapp.core.ui.components.CafeTopAppBar
import com.cafeapp.core.ui.components.StatusBadge
import com.cafeapp.core.ui.components.VegIndicator
import com.cafeapp.core.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOrdersScreen(
    viewModel: CustomerOrdersViewModel = hiltViewModel(),
    onNavigateToMenu: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val filteredOrders = when (uiState.activeTab) {
        "ACTIVE" -> uiState.orders.filter {
            it.status.uppercase() in listOf("PLACED", "ACCEPTED", "PREPARING", "READY")
        }
        "COMPLETED" -> uiState.orders.filter {
            it.status.uppercase() in listOf("SERVED", "COMPLETED", "CANCELLED")
        }
        else -> uiState.orders
    }

    Scaffold(
        topBar = {
            CafeTopAppBar(
                title = "My Orders",
                subtitle = "Track orders & view history",
                onNavigateBack = onNavigateBack,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            val tabs = listOf("ALL" to "All Orders", "ACTIVE" to "Active", "COMPLETED" to "Past Orders")
            val selectedTabIndex = tabs.indexOfFirst { it.first == uiState.activeTab }.coerceAtLeast(0)

            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                tabs.forEachIndexed { index, (key, label) ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { viewModel.setActiveTab(key) },
                        text = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            )
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
                        CafeLoadingIndicator(message = "Fetching your orders...")
                    }
                    uiState.errorMessage != null -> {
                        CafeErrorState(
                            message = uiState.errorMessage ?: "Failed to load orders",
                            onRetry = { viewModel.loadOrders() }
                        )
                    }
                    filteredOrders.isEmpty() -> {
                        CafeEmptyState(
                            title = if (uiState.activeTab == "ACTIVE") "No active orders" else "No orders yet",
                            description = if (uiState.activeTab == "ACTIVE") "You have no orders in preparation right now." else "Place your first order to experience our freshly brewed coffees & artisan bakery!",
                            actionLabel = "Browse Menu",
                            onAction = onNavigateToMenu,
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
                                OrderCard(order = order)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(
    order: OrderDto,
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
            // Header: Order number and status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
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

            // Sub-info: Date & Table
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
                        text = formatOrderDate(order.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                val orderTable = order.table
                val tableId = order.tableId
                if (orderTable != null || tableId != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TableBar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(Dimens.space4))
                        Text(
                            text = if (orderTable != null) "Table #${orderTable.tableNumber}" else "Dine-in",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.space12))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Dimens.space12))

            // Items breakdown
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.space6)) {
                order.items.forEach { item ->
                    val menuItem = item.menuItem
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            VegIndicator(isVeg = menuItem?.isVeg ?: true, size = 10.dp)
                            Spacer(modifier = Modifier.width(Dimens.space8))
                            Text(
                                text = "${item.quantity}x ${menuItem?.name ?: "Item"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }

                        Text(
                            text = CurrencyFormatter.formatPricePaise(item.subtotalPaise),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            val instructions = order.specialInstructions
            if (!instructions.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Dimens.space8))
                Text(
                    text = "Note: $instructions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.space12))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Dimens.space12))

            // Total Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Paid / Due",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = CurrencyFormatter.formatPricePaise(order.totalPaise),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun formatOrderDate(isoString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val date = inputFormat.parse(isoString.take(19)) ?: return isoString
        val outputFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        isoString
    }
}
