package com.cafeapp.feature.cafe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafeapp.core.network.dto.CafeDto
import com.cafeapp.core.network.dto.MenuItemDto
import com.cafeapp.core.network.dto.OfferDto
import com.cafeapp.core.ui.CurrencyFormatter
import com.cafeapp.core.ui.components.CafeButton
import com.cafeapp.core.ui.components.CafeErrorState
import com.cafeapp.core.ui.components.CafeLoadingIndicator
import com.cafeapp.core.ui.components.CafeSectionHeader
import com.cafeapp.core.ui.components.PromoBadge
import com.cafeapp.core.ui.components.StatusBadge
import com.cafeapp.core.ui.components.VegIndicator
import com.cafeapp.core.ui.theme.CafeBrown
import com.cafeapp.core.ui.theme.CafeGold
import com.cafeapp.core.ui.theme.Dimens
import com.cafeapp.core.ui.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    viewModel: CustomerHomeViewModel = hiltViewModel(),
    onNavigateToMenu: () -> Unit = {},
    onNavigateToOffers: () -> Unit = {},
    onNavigateToReservations: () -> Unit = {},
    onNavigateToQrScanner: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToOrders: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cartState by viewModel.cartState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.notificationMessage, uiState.errorMessage) {
        uiState.notificationMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotification()
        }
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (cartState.items.isNotEmpty()) {
                FloatingCartBar(
                    totalItems = cartState.totalItemCount,
                    totalPaise = cartState.totalPaise,
                    onViewCart = onNavigateToCart,
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.cafe == null) {
                CafeLoadingIndicator(message = "Preparing your cafe experience...")
            } else if (uiState.errorMessage != null && uiState.cafe == null) {
                CafeErrorState(
                    message = uiState.errorMessage ?: "Failed to load cafe details",
                    onRetry = { viewModel.loadHomeData() }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = if (cartState.items.isNotEmpty()) 80.dp else 24.dp)
                ) {
                    // Hero Greeting & Table Status
                    item {
                        HomeHeroHeader(
                            userName = uiState.userName,
                            tableNumber = uiState.tableNumber,
                            cafe = uiState.cafe,
                            onScanTable = onNavigateToQrScanner,
                        )
                    }

                    // Quick Actions Row
                    item {
                        QuickActionsSection(
                            onScanQr = onNavigateToQrScanner,
                            onBookTable = onNavigateToReservations,
                            onBrowseMenu = onNavigateToMenu,
                            onSpecialDeals = onNavigateToOffers,
                        )
                    }

                    // Active Offers Carousel
                    if (uiState.offers.isNotEmpty()) {
                        item {
                            CafeSectionHeader(
                                title = "Special Offers & Deals",
                                actionLabel = "View All (${uiState.offers.size})",
                                onActionClick = onNavigateToOffers,
                            )
                        }

                        item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = Dimens.space16),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.space12)
                            ) {
                                items(uiState.offers, key = { it.id }) { offer ->
                                    OfferHomeCard(
                                        offer = offer,
                                        onApply = { viewModel.applyOffer(offer) }
                                    )
                                }
                            }
                        }
                    }

                    // Popular / Recommended Items
                    if (uiState.popularItems.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(Dimens.space16))
                            CafeSectionHeader(
                                title = "Popular & Chef's Specials",
                                actionLabel = "Full Menu",
                                onActionClick = onNavigateToMenu,
                            )
                        }

                        items(uiState.popularItems, key = { it.id }) { item ->
                            PopularItemRow(
                                item = item,
                                onAddToCart = { viewModel.addToCart(item) }
                            )
                        }
                    }

                    // Cafe Story & Info Footer Card
                    item {
                        Spacer(modifier = Modifier.height(Dimens.space16))
                        CafeInfoFooter(cafe = uiState.cafe)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeroHeader(
    userName: String?,
    tableNumber: Int?,
    cafe: CafeDto?,
    onScanTable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.surface,
                    )
                )
            )
            .padding(
                start = Dimens.space16,
                end = Dimens.space16,
                top = Dimens.space24,
                bottom = Dimens.space16
            )
    ) {
        Column {
            // User Greeting & Seated Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (!userName.isNullOrBlank()) "Hello, $userName 👋" else "Welcome to",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        text = cafe?.name ?: "The Roast & Toast Cafe",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    )
                }

                // Table Status Badge
                Surface(
                    shape = RoundedCornerShape(Dimens.radiusLarge),
                    color = if (tableNumber != null) CafeGold else MaterialTheme.colorScheme.surface.copy(alpha = 0.25f),
                    modifier = Modifier.clickable { onScanTable() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Dimens.space12, vertical = Dimens.space6),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableBar,
                            contentDescription = null,
                            tint = if (tableNumber != null) Color.Black else MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(Dimens.space4))
                        Text(
                            text = if (tableNumber != null) "Table #$tableNumber" else "Scan Table",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (tableNumber != null) Color.Black else MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.space16))

            // Operational Status Pill Card
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.radiusMedium),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.space12),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (cafe?.isAcceptingOrders != false) StatusSuccess else Color.Red)
                        )
                        Spacer(modifier = Modifier.width(Dimens.space8))
                        Column {
                            Text(
                                text = if (cafe?.isAcceptingOrders != false) "Open Now • Accepting Orders" else "Currently Closed",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (cafe?.isAcceptingOrders != false) StatusSuccess else Color.Red,
                            )
                            Text(
                                text = "Hours: ${cafe?.openingTime ?: "08:00 AM"} - ${cafe?.closingTime ?: "11:00 PM"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    onScanQr: () -> Unit,
    onBookTable: () -> Unit,
    onBrowseMenu: () -> Unit,
    onSpecialDeals: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.space16, vertical = Dimens.space8)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space12)
        ) {
            QuickActionButton(
                icon = Icons.Default.QrCodeScanner,
                label = "Scan QR",
                sublabel = "Table Dine-in",
                onClick = onScanQr,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            QuickActionButton(
                icon = Icons.Default.EventSeat,
                label = "Book Table",
                sublabel = "Reserve Seat",
                onClick = onBookTable,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }

        Spacer(modifier = Modifier.height(Dimens.space12))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.space12)
        ) {
            QuickActionButton(
                icon = Icons.Default.RestaurantMenu,
                label = "Order Food",
                sublabel = "Browse Menu",
                onClick = onBrowseMenu,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )

            QuickActionButton(
                icon = Icons.Default.Discount,
                label = "Deals",
                sublabel = "Promo Coupons",
                onClick = onSpecialDeals,
                modifier = Modifier.weight(1f),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    sublabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.radiusMedium),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.space12),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(contentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(Dimens.space8))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = contentColor,
                )
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = contentColor.copy(alpha = 0.75f),
                )
            }
        }
    }
}

@Composable
fun OfferHomeCard(
    offer: OfferDto,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val discountText = if (offer.discountType == "PERCENTAGE") "${offer.discountValue}% OFF" else "₹${offer.discountValue / 100} OFF"
    val offerDesc = offer.description
    Card(
        modifier = modifier.width(260.dp),
        shape = RoundedCornerShape(Dimens.radiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.space12)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PromoBadge(code = offer.code)
                Text(
                    text = discountText,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = StatusSuccess,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.space8))

            val offerTitle = offer.title
            Text(
                text = if (!offerTitle.isNullOrBlank()) offerTitle else offer.code,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = offerDesc ?: "Valid on all orders above ₹${offer.minOrderAmountPaise / 100}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(Dimens.space8))

            OutlinedButton(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Dimens.radiusSmall),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text("Apply Coupon", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PopularItemRow(
    item: MenuItemDto,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.space16, vertical = Dimens.space4),
        shape = RoundedCornerShape(Dimens.radiusMedium),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                VegIndicator(isVeg = item.isVeg)
                Spacer(modifier = Modifier.width(Dimens.space8))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (item.displayOrder <= 2) {
                            Spacer(modifier = Modifier.width(Dimens.space4))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Popular",
                                tint = CafeGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    val itemDesc = item.description
                    if (!itemDesc.isNullOrBlank()) {
                        Text(
                            text = itemDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = CurrencyFormatter.formatPricePaise(item.pricePaise),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Button(
                onClick = onAddToCart,
                shape = RoundedCornerShape(Dimens.radiusSmall),
                contentPadding = PaddingValues(horizontal = Dimens.space12, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(Dimens.space4))
                Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CafeInfoFooter(
    cafe: CafeDto?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.space16),
        shape = RoundedCornerShape(Dimens.radiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        )
    ) {
        Column(modifier = Modifier.padding(Dimens.space16)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Coffee,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(Dimens.space8))
                Text(
                    text = "About ${cafe?.name ?: "The Roast & Toast Cafe"}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.space8))

            Text(
                text = cafe?.description ?: "Artisanal espresso bar & gourmet kitchen delivering handcrafted specialty coffee, sourdough toasts, and fresh bakery treats.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Dimens.space12))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(Dimens.space12))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(Dimens.space6))
                Text(
                    text = cafe?.address ?: "Connaught Place, New Delhi, India",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val cafePhone = cafe?.phone
            if (!cafePhone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Dimens.space6))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(Dimens.space6))
                    Text(
                        text = cafePhone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingCartBar(
    totalItems: Int,
    totalPaise: Int,
    onViewCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.space16),
        shape = RoundedCornerShape(Dimens.radiusMedium),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primary,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onViewCart() }
                .padding(horizontal = Dimens.space16, vertical = Dimens.space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "$totalItems Item${if (totalItems > 1) "s" else ""} added",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                )
                Text(
                    text = CurrencyFormatter.formatPricePaise(totalPaise),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "View Cart",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(Dimens.space8))
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
