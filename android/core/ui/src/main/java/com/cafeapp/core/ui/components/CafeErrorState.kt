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
import com.cafeapp.core.ui.theme.StatusError

@Composable
fun CafeErrorState(
    message: String,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    onRetry: (() -> Unit)? = null,
    retryButtonText: String = "Try Again",
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.screenPaddingHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = StatusError,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Dimens.space8))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(Dimens.space24))
            CafeButton(
                text = retryButtonText,
                onClick = onRetry,
                variant = CafeButtonVariant.OUTLINED,
                modifier = Modifier.fillMaxWidth(0.6f),
            )
        }
    }
}
