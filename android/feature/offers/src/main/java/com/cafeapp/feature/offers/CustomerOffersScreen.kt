package com.cafeapp.feature.offers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafeapp.core.network.dto.OfferDto
import com.cafeapp.core.ui.components.CafeEmptyState
import com.cafeapp.core.ui.components.CafeErrorState
import com.cafeapp.core.ui.components.CafeLoadingIndicator
import com.cafeapp.core.ui.components.CafeTopAppBar
import com.cafeapp.core.ui.components.PromoBadge
import com.cafeapp.core.ui.theme.Dimens
import com.cafeapp.core.ui.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerOffersScreen(
    viewModel: CustomerOffersViewModel = hiltViewModel(),
    onNavigateBack: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cartState by viewModel.cartState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        topBar = {
            CafeTopAppBar(
                title = "Special Offers & Deals",
                subtitle = "Exclusive cafe savings",
                onNavigateBack = onNavigateBack,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CafeLoadingIndicator(message = "Checking best offers...")
                }
                uiState.errorMessage != null -> {
                    CafeErrorState(
                        message = uiState.errorMessage ?: "Failed to load offers",
                        onRetry = { viewModel.loadOffers() },
                    )
                }
                uiState.offers.isEmpty() -> {
                    CafeEmptyState(
                        title = "No active offers",
                        description = "Check back soon for seasonal discounts and perks!",
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Dimens.space16),
                        verticalArrangement = Arrangement.spacedBy(Dimens.space16),
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(Dimens.radiusMedium))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(Dimens.space16)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Discount,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(end = Dimens.space12),
                                    )
                                    Column {
                                        Text(
                                            text = "Save big on every order!",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                        Text(
                                            text = "Tap 'Apply' or copy the promo code to get instant savings.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = uiState.offers,
                            key = { it.id }
                        ) { offer ->
                            val isApplied = cartState.appliedOffer?.code == offer.code
                            OfferCard(
                                offer = offer,
                                isApplied = isApplied,
                                onClick = { viewModel.showOfferDetails(offer) },
                                onApply = { viewModel.applyOffer(offer) },
                                onCopyCode = {
                                    clipboardManager.setText(AnnotatedString(offer.code))
                                    viewModel.onCodeCopied(offer.code)
                                }
                            )
                        }
                    }
                }
            }

            // Offer Detail Bottom Sheet
            uiState.selectedOfferForDetails?.let { offer ->
                val isApplied = cartState.appliedOffer?.code == offer.code
                OfferDetailBottomSheet(
                    offer = offer,
                    isApplied = isApplied,
                    onDismiss = { viewModel.dismissOfferDetails() },
                    onApply = { viewModel.applyOffer(offer) },
                    onCopyCode = {
                        clipboardManager.setText(AnnotatedString(offer.code))
                        viewModel.onCodeCopied(offer.code)
                    }
                )
            }
        }
    }
}

@Composable
fun OfferCard(
    offer: OfferDto,
    isApplied: Boolean,
    onClick: () -> Unit,
    onApply: () -> Unit,
    onCopyCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(Dimens.radiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isApplied) BorderStroke(1.5.dp, StatusSuccess) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                // Promo Code Box with Copy
                Row(
                    modifier = Modifier
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(Dimens.radiusSmall)
                        )
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            RoundedCornerShape(Dimens.radiusSmall)
                        )
                        .clickable(onClick = onCopyCode)
                        .padding(horizontal = Dimens.space8, vertical = Dimens.space4),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = offer.code,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(Dimens.space4))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Discount tag
                val discountLabel = if (offer.discountType == "PERCENTAGE") {
                    "${offer.discountValue}% OFF"
                } else {
                    "₹${offer.discountValue / 100} OFF"
                }
                PromoBadge(text = discountLabel)
            }

            Spacer(modifier = Modifier.height(Dimens.space12))

            val offerTitle = offer.title
            if (!offerTitle.isNullOrBlank()) {
                Text(
                    text = offerTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(Dimens.space4))
            }

            val offerDesc = offer.description
            if (!offerDesc.isNullOrBlank()) {
                Text(
                    text = offerDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(Dimens.space8))
            }

            // Terms & Min order
            if (offer.minOrderAmountPaise > 0) {
                Text(
                    text = "• Min. order ₹${offer.minOrderAmountPaise / 100}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val maxDisc = offer.maxDiscountAmountPaise
            if (maxDisc != null && maxDisc > 0) {
                Text(
                    text = "• Max discount ₹${maxDisc / 100}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.space12))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(onClick = onClick)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(Dimens.space4))
                    Text(
                        text = "View details",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isApplied) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = Dimens.space8)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Applied",
                            tint = StatusSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Dimens.space4))
                        Text(
                            text = "Applied",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = StatusSuccess,
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onApply,
                        shape = RoundedCornerShape(Dimens.radiusSmall),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    ) {
                        Text(
                            text = "Apply Code",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }
    }
}
