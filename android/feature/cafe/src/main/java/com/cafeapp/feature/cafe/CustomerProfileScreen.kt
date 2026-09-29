package com.cafeapp.feature.cafe

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cafeapp.core.network.dto.CafeDto
import com.cafeapp.core.ui.components.CafeButton
import com.cafeapp.core.ui.components.CafeButtonVariant
import com.cafeapp.core.ui.components.CafeErrorState
import com.cafeapp.core.ui.components.CafeLoadingIndicator
import com.cafeapp.core.ui.components.CafeTopAppBar
import com.cafeapp.core.ui.components.StatusBadge
import com.cafeapp.core.ui.theme.CafeGold
import com.cafeapp.core.ui.theme.Dimens
import com.cafeapp.core.ui.theme.StatusError
import com.cafeapp.core.ui.theme.StatusSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    viewModel: CustomerProfileViewModel = hiltViewModel(),
    onNavigateToQrScanner: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out from The Roast & Toast Cafe?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                CafeButton(
                    text = "Sign Out",
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout(onSuccess = onLogout)
                    },
                    variant = CafeButtonVariant.DESTRUCTIVE,
                    isLoading = uiState.isLoggingOut,
                )
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CafeTopAppBar(
                title = "My Profile",
                subtitle = "Account & Cafe details",
                onNavigateBack = onNavigateBack,
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading && uiState.userName == null) {
                CafeLoadingIndicator(message = "Loading your account profile...")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimens.space16),
                    verticalArrangement = Arrangement.spacedBy(Dimens.space16)
                ) {
                    // User Identity Hero Card
                    item {
                        UserProfileCard(
                            userName = uiState.userName,
                            userEmail = uiState.userEmail,
                            userRole = uiState.userRole,
                            userId = uiState.userId,
                        )
                    }

                    // Table Seating Card
                    item {
                        TableSeatingCard(
                            tableNumber = uiState.tableNumber,
                            onScanTable = onNavigateToQrScanner,
                            onLeaveTable = { viewModel.leaveTable() }
                        )
                    }

                    // Cafe Information Card
                    item {
                        CafeDetailsCard(cafe = uiState.cafe)
                    }

                    // App Information Card
                    item {
                        AppInfoCard()
                    }

                    // Sign Out Button
                    item {
                        Spacer(modifier = Modifier.height(Dimens.space8))
                        OutlinedButton(
                            onClick = { showLogoutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Dimens.buttonHeight),
                            shape = RoundedCornerShape(Dimens.radiusMedium),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = StatusError,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = null,
                                tint = StatusError,
                                modifier = Modifier.size(Dimens.buttonIconSize),
                            )
                            Spacer(modifier = Modifier.width(Dimens.space8))
                            Text(
                                text = "Sign Out",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = StatusError,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileCard(
    userName: String?,
    userEmail: String?,
    userRole: String?,
    userId: String?,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusMedium),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.space16)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName?.take(1)?.uppercase() ?: "C",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }

                Spacer(modifier = Modifier.width(Dimens.space16))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName ?: "Customer",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = userEmail ?: "customer@cafeapp.local",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                StatusBadge(status = userRole ?: "CUSTOMER")
            }

            if (!userId.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Dimens.space12))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(Dimens.space8))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "User ID",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = userId.take(12) + "...",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
fun TableSeatingCard(
    tableNumber: Int?,
    onScanTable: () -> Unit,
    onLeaveTable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = if (tableNumber != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.space16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.TableBar,
                    contentDescription = null,
                    tint = if (tableNumber != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(Dimens.space12))
                Column {
                    Text(
                        text = if (tableNumber != null) "Seated at Table #$tableNumber" else "Not Seated at a Table",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (tableNumber != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (tableNumber != null) "Orders will be delivered here" else "Scan QR code on your table for dine-in",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (tableNumber != null) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    )
                }
            }

            if (tableNumber != null) {
                TextButton(onClick = onLeaveTable) {
                    Text("Leave Table", color = StatusError, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onScanTable) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan")
                }
            }
        }
    }
}

@Composable
fun CafeDetailsCard(
    cafe: CafeDto?,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusMedium),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.space16)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Coffee,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Dimens.space8))
                Text(
                    text = cafe?.name ?: "The Roast & Toast Cafe",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.space12))

            ProfileInfoRow(
                icon = Icons.Default.LocationOn,
                label = "Address",
                value = cafe?.address ?: "Connaught Place, New Delhi, India"
            )

            val phone = cafe?.phone
            if (!phone.isNullOrBlank()) {
                ProfileInfoRow(
                    icon = Icons.Default.Phone,
                    label = "Phone",
                    value = phone
                )
            }

            val email = cafe?.email
            if (!email.isNullOrBlank()) {
                ProfileInfoRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = email
                )
            }

            ProfileInfoRow(
                icon = Icons.Default.AccessTime,
                label = "Operating Hours",
                value = "${cafe?.openingTime ?: "08:00 AM"} - ${cafe?.closingTime ?: "11:00 PM"}"
            )

            val gstin = cafe?.gstin
            if (!gstin.isNullOrBlank()) {
                ProfileInfoRow(
                    icon = Icons.Default.Receipt,
                    label = "GSTIN",
                    value = gstin
                )
            }
        }
    }
}

@Composable
fun ProfileInfoRow(
    icon: ImageVector,
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(Dimens.space8))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun AppInfoCard(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.radiusMedium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.space16)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Dimens.space8))
                Text(
                    text = "Application Information",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.space8))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "App Version",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "1.0.0 (Phase 5 Release)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.space4))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Backend Server",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "http://10.0.2.2:3000/api/v1",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = StatusSuccess,
                )
            }
        }
    }
}
