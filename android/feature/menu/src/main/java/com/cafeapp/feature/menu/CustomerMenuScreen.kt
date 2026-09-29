package com.cafeapp.feature.menu

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafeapp.core.network.dto.MenuItemDto
import com.cafeapp.core.ui.components.CafeEmptyState
import com.cafeapp.core.ui.components.CafeErrorState
import com.cafeapp.core.ui.components.CafeLoadingIndicator
import com.cafeapp.core.ui.components.CafeTopAppBar
import com.cafeapp.core.ui.components.VegIndicator
import com.cafeapp.core.ui.formatPricePaise
import com.cafeapp.core.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerMenuScreen(
    viewModel: CustomerMenuViewModel = hiltViewModel(),
    onNavigateToCart: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cartState by viewModel.cartState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CafeTopAppBar(
                title = "Menu",
                subtitle = "Handcrafted with passion",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = onNavigateToCart) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                            if (cartState.totalItemCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.TopEnd)
                                        .background(MaterialTheme.colorScheme.error, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = cartState.totalItemCount.toString(),
                                        color = MaterialTheme.colorScheme.onError,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (cartState.totalItemCount > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.space16)
                            .clip(RoundedCornerShape(Dimens.radiusMedium))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(onClick = onNavigateToCart)
                            .padding(horizontal = Dimens.space16, vertical = Dimens.space12),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "${cartState.totalItemCount} item${if (cartState.totalItemCount > 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                            )
                            Text(
                                text = formatPricePaise(cartState.totalPaise),
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
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(Dimens.buttonIconSize),
                            )
                        }
                    }
                }
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
                    CafeLoadingIndicator(message = "Brewing menu items...")
                }
                uiState.errorMessage != null -> {
                    CafeErrorState(
                        message = uiState.errorMessage ?: "Failed to load menu",
                        onRetry = { viewModel.loadMenuData() },
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search Bar & Filter Row
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = Dimens.space16, vertical = Dimens.space8)
                        ) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Search coffee, drinks, snacks...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                },
                                trailingIcon = {
                                    if (uiState.searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(Dimens.radiusMedium),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                )
                            )

                            Spacer(modifier = Modifier.height(Dimens.space8))

                            // Horizontal Filter & Category row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.space8),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Veg Only Filter Chip
                                FilterChip(
                                    selected = uiState.isVegOnly,
                                    onClick = { viewModel.toggleVegOnly() },
                                    label = { Text("Veg Only", fontSize = 12.sp) },
                                    leadingIcon = { VegIndicator(isVeg = true) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE8F5E9),
                                        selectedLabelColor = Color(0xFF2E7D32),
                                    )
                                )

                                // Category "All"
                                SuggestionChip(
                                    onClick = { viewModel.selectCategory(null) },
                                    label = { Text("All Items", fontSize = 12.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (uiState.selectedCategoryId == null)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = if (uiState.selectedCategoryId == null)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                )

                                // Other Categories
                                uiState.categories.forEach { category ->
                                    val isSelected = uiState.selectedCategoryId == category.id
                                    SuggestionChip(
                                        onClick = { viewModel.selectCategory(category.id) },
                                        label = { Text(category.name, fontSize = 12.sp) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (isSelected)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            labelColor = if (isSelected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    )
                                }
                            }
                        }

                        // Menu Items List
                        if (uiState.filteredItems.isEmpty()) {
                            CafeEmptyState(
                                title = "No items found",
                                description = "Try changing your search term or category filter",
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = Dimens.space16,
                                    end = Dimens.space16,
                                    top = Dimens.space8,
                                    bottom = if (cartState.totalItemCount > 0) 80.dp else Dimens.space16,
                                ),
                                verticalArrangement = Arrangement.spacedBy(Dimens.space12),
                            ) {
                                items(
                                    items = uiState.filteredItems,
                                    key = { it.id }
                                ) { menuItem ->
                                    val itemInCart = cartState.items.find { it.menuItemId == menuItem.id }
                                    val quantity = itemInCart?.quantity ?: 0

                                    MenuItemCard(
                                        item = menuItem,
                                        quantity = quantity,
                                        onAdd = { viewModel.addToCart(menuItem) },
                                        onRemove = { viewModel.decrementCartItem(menuItem.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuItemCard(
    item: MenuItemDto,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.space16),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VegIndicator(isVeg = item.isVeg ?: true)
                    Spacer(modifier = Modifier.width(Dimens.space8))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val itemDesc = item.description
                if (!itemDesc.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(Dimens.space4))
                    Text(
                        text = itemDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.space8))
                Text(
                    text = formatPricePaise(item.pricePaise),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.width(Dimens.space16))

            // Action / Quantity Controls
            if (!item.isAvailable) {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(Dimens.radiusSmall)
                        )
                        .padding(horizontal = Dimens.space8, vertical = Dimens.space4)
                ) {
                    Text(
                        text = "Sold Out",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (quantity == 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.radiusSmall))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onAdd)
                        .padding(horizontal = Dimens.space16, vertical = Dimens.space8),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(Dimens.space4))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(Dimens.radiusSmall))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = Dimens.space4, vertical = Dimens.space2),
                ) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Text(
                        text = quantity.toString(),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = Dimens.space8),
                    )

                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
