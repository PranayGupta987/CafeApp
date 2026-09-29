package com.cafeapp.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.cafeapp.core.ui.theme.Dimens

@Composable
fun CafeEmptyState(
    title: String,
    message: String = "",
    description: String? = null,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    actionButtonText: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    onAction: (() -> Unit)? = null,
) {
    val effectiveMessage = description ?: message
    val effectiveActionText = actionLabel ?: actionButtonText
    val effectiveOnAction = onAction ?: onActionClick

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.screenPaddingHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.height(Dimens.space16))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (effectiveMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(Dimens.space8))
            Text(
                text = effectiveMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (effectiveActionText != null && effectiveOnAction != null) {
            Spacer(modifier = Modifier.height(Dimens.space24))
            CafeButton(
                text = effectiveActionText,
                onClick = effectiveOnAction,
                variant = CafeButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth(0.6f),
            )
        }
    }
}
