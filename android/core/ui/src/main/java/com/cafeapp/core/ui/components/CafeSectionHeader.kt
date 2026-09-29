package com.cafeapp.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.cafeapp.core.ui.theme.Dimens

@Composable
fun CafeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    actionLabel: String? = actionText,
    onActionClick: (() -> Unit)? = null,
) {
    val displayAction = actionLabel ?: actionText
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.space8),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (displayAction != null && onActionClick != null) {
            Text(
                text = displayAction,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onActionClick),
            )
        }
    }
}
