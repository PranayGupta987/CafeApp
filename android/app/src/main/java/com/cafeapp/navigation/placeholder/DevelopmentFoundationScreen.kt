package com.cafeapp.navigation.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cafeapp.core.ui.components.CafeButton
import com.cafeapp.core.ui.components.CafeButtonVariant
import com.cafeapp.core.ui.components.CafeCard
import com.cafeapp.core.ui.theme.CafeBrown
import com.cafeapp.core.ui.theme.CafeGold
import com.cafeapp.core.ui.theme.Dimens
import com.cafeapp.core.ui.theme.StatusError
import com.cafeapp.core.ui.theme.StatusSuccess

@Composable
fun DevelopmentFoundationScreen(
    onNavigateToAuth: () -> Unit,
    viewModel: DevelopmentFoundationViewModel = hiltViewModel(),
) {
    val healthState by viewModel.healthState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(Dimens.screenPaddingHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(Dimens.space24))

            // Header Banner
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.space20),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "☕ CafeApp",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(Dimens.space4))
                    Text(
                        text = "Phase 2 — Development Build Foundation",
                        style = MaterialTheme.typography.titleMedium,
                        color = CafeGold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.space16))

            // Backend Health Status Card
            CafeCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "🔌 Backend Health Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(Dimens.space12))

                when (val state = healthState) {
                    is HealthState.Idle -> {
                        Text(
                            text = "Awaiting health check...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is HealthState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(Dimens.space12))
                            Text(
                                text = "Checking http://10.0.2.2:3000/api/v1/health...",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    is HealthState.Success -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(StatusSuccess),
                            )
                            Spacer(modifier = Modifier.width(Dimens.space8))
                            Text(
                                text = "Connected: ${state.response.service ?: "CafeApp Backend"}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusSuccess,
                            )
                        }
                        Spacer(modifier = Modifier.height(Dimens.space4))
                        Text(
                            text = "Status: ${state.response.status} | DB: ${state.response.database ?: "connected"} | Env: ${state.response.environment ?: "dev"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is HealthState.Error -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(StatusError),
                            )
                            Spacer(modifier = Modifier.width(Dimens.space8))
                            Text(
                                text = "Backend Unreachable (10.0.2.2:3000)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusError,
                            )
                        }
                        Spacer(modifier = Modifier.height(Dimens.space4))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.space12))
                CafeButton(
                    text = "Re-check Health",
                    onClick = { viewModel.checkBackendHealth() },
                    variant = CafeButtonVariant.OUTLINED,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(Dimens.space16))

            // Architecture Foundation Status Card
            CafeCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "🏗️ Foundation Modules & Invariants",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(Dimens.space12))

                FoundationItem(title = "🎨 Material 3 Design System", detail = "CafeBrown, CafeCream, CafeGold, Typography, Dimens, Shapes, CafeButton, CafeCard")
                FoundationItem(title = "🌐 Network & AuthInterceptor", detail = "Retrofit 2.11, OkHttp 4.12, Moshi, TokenAuthenticator")
                FoundationItem(title = "🗄️ Database Foundation", detail = "Room 2.6.1 SQLite database with singleton Hilt provider")
                FoundationItem(title = "⚡ Real-time WebSocket", detail = "Socket.IO client 2.1.0 ready for order/kitchen channels")
                FoundationItem(title = "📌 Product Invariants", detail = "Dine-in only, Single Cafe singleton, Integer Paise INR (₹), Asia/Kolkata TZ")
            }

            Spacer(modifier = Modifier.height(Dimens.space20))

            // Primary Navigation Action
            CafeButton(
                text = "Continue to Auth / App Flow",
                onClick = onNavigateToAuth,
                variant = CafeButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(Dimens.space24))
        }
    }
}

@Composable
private fun FoundationItem(title: String, detail: String) {
    Column(modifier = Modifier.padding(vertical = Dimens.space4)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
