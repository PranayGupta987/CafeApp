package com.cafeapp.navigation.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeapp.core.ui.components.CafeButton
import com.cafeapp.core.ui.components.CafeButtonVariant
import com.cafeapp.core.ui.components.CafeCard
import com.cafeapp.core.ui.theme.CafeGold
import com.cafeapp.core.ui.theme.Dimens
import com.cafeapp.core.ui.theme.StatusWarning
import com.cafeapp.feature.auth.ui.AuthEvent
import com.cafeapp.feature.auth.ui.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CafeHomePlaceholder(
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    viewModel: CafeHomeViewModel = hiltViewModel(),
) {
    val userName by viewModel.sessionPreferences.userName.collectAsState(initial = "Staff/Admin")
    val userEmail by viewModel.sessionPreferences.userEmail.collectAsState(initial = "")
    val userRole by viewModel.sessionPreferences.userRole.collectAsState(initial = "CAFE_ADMIN")
    val userId by viewModel.sessionPreferences.userId.collectAsState(initial = "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("👔 Cafe Management", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(Dimens.screenPaddingHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(Dimens.space16))

            // User Profile Header Card
            CafeCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(CafeGold.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Staff Avatar",
                            tint = CafeGold,
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(Dimens.space16))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName ?: "Staff / Admin",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (!userEmail.isNullOrBlank()) {
                            Text(
                                text = userEmail!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Surface(
                        color = StatusWarning.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = userRole ?: "STAFF",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusWarning,
                            modifier = Modifier.padding(horizontal = Dimens.space8, vertical = Dimens.space4),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.space20))

            // Information Card
            CafeCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "💼 Staff & Admin Operations Hub",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(Dimens.space8))
                Text(
                    text = "Authorized access for Table Session approvals, Order status updates, and Offer management.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(Dimens.space12))
                Text(
                    text = "User ID: ${userId ?: "Loaded"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Text(
                    text = "Role: ${userRole ?: "CAFE_ADMIN"} (RBAC Verified)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout Action Button
            CafeButton(
                text = "Log Out",
                onClick = {
                    authViewModel.onEvent(AuthEvent.LogoutClicked)
                    onLogout()
                },
                variant = CafeButtonVariant.DESTRUCTIVE,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Dimens.space24))
        }
    }
}
