package com.cafeapp.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cafeapp.core.ui.theme.Dimens
import com.cafeapp.core.ui.theme.StatusError
import com.cafeapp.core.ui.theme.StatusInfo
import com.cafeapp.core.ui.theme.StatusSuccess
import com.cafeapp.core.ui.theme.StatusWarning

@Composable
fun VegIndicator(
    isVeg: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    val borderColor = if (isVeg) Color(0xFF2E7D32) else Color(0xFFC62828)
    val dotColor = if (isVeg) Color(0xFF2E7D32) else Color(0xFFC62828)

    Box(
        modifier = modifier
            .size(size)
            .border(1.5.dp, borderColor, RoundedCornerShape(3.dp))
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.375f)
                .background(dotColor, CircleShape)
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier,
    size: Dp? = null,
) {
    val (bgColor, textColor) = when (status.uppercase()) {
        "PLACED", "PENDING" -> Pair(StatusInfo.copy(alpha = 0.15f), StatusInfo)
        "ACCEPTED", "CONFIRMED" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "PREPARING" -> Pair(StatusWarning.copy(alpha = 0.15f), Color(0xFFE65100))
        "READY", "SEATED" -> Pair(Color(0xFFE1F5FE), Color(0xFF0277BD))
        "SERVED", "COMPLETED" -> Pair(StatusSuccess.copy(alpha = 0.15f), StatusSuccess)
        "CANCELLED", "REJECTED", "NO_SHOW" -> Pair(StatusError.copy(alpha = 0.15f), StatusError)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    val displayStatus = when (status.uppercase()) {
        "NO_SHOW" -> "No Show"
        else -> status.lowercase().replaceFirstChar { it.uppercase() }
    }

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(Dimens.radiusSmall))
            .padding(horizontal = Dimens.space8, vertical = Dimens.space4),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayStatus,
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
            ),
        )
    }
}

@Composable
fun PromoBadge(
    text: String = "",
    code: String = text,
    modifier: Modifier = Modifier,
) {
    val displayText = if (code.isNotBlank()) code else text
    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(Dimens.radiusSmall)
            )
            .padding(horizontal = Dimens.space8, vertical = Dimens.space4),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayText,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        )
    }
}
