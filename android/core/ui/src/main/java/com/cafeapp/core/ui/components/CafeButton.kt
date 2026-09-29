package com.cafeapp.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cafeapp.core.ui.theme.CafeBrown
import com.cafeapp.core.ui.theme.CafeGold
import com.cafeapp.core.ui.theme.Dimens
import com.cafeapp.core.ui.theme.StatusError

enum class CafeButtonVariant {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    TEXT,
    DESTRUCTIVE,
}

@Composable
fun CafeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CafeButtonVariant = CafeButtonVariant.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val buttonColors = when (variant) {
        CafeButtonVariant.PRIMARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CafeButtonVariant.SECONDARY -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CafeButtonVariant.DESTRUCTIVE -> ButtonDefaults.buttonColors(
            containerColor = StatusError,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CafeButtonVariant.OUTLINED, CafeButtonVariant.TEXT -> ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
        )
    }

    when (variant) {
        CafeButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier.height(Dimens.buttonHeight),
                enabled = enabled && !isLoading,
                shape = MaterialTheme.shapes.medium,
            ) {
                ButtonInnerContent(
                    text = text,
                    isLoading = isLoading,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                )
            }
        }
        CafeButtonVariant.TEXT -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.height(Dimens.buttonHeight),
                enabled = enabled && !isLoading,
                shape = MaterialTheme.shapes.medium,
            ) {
                ButtonInnerContent(
                    text = text,
                    isLoading = isLoading,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                )
            }
        }
        else -> {
            Button(
                onClick = onClick,
                modifier = modifier.height(Dimens.buttonHeight),
                enabled = enabled && !isLoading,
                colors = buttonColors,
                shape = MaterialTheme.shapes.medium,
            ) {
                ButtonInnerContent(
                    text = text,
                    isLoading = isLoading,
                    indicatorColor = when (variant) {
                        CafeButtonVariant.DESTRUCTIVE -> Color.White
                        else -> MaterialTheme.colorScheme.onPrimary
                    },
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                )
            }
        }
    }
}

@Composable
private fun ButtonInnerContent(
    text: String,
    isLoading: Boolean,
    indicatorColor: Color,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(Dimens.buttonIconSize),
            color = indicatorColor,
            strokeWidth = 2.dp,
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(Dimens.space8))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
            )
            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(Dimens.space8))
                trailingIcon()
            }
        }
    }
}
