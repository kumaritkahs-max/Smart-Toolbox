package com.githubcontrol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.githubcontrol.utils.AppError

/**
 * Reusable error card. Pass the friendly message + an optional retry handler.
 * Consistent look across every screen so users get the same recovery signal.
 */
@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    actionLabel: String = "Retry"
) {
    val errBg = MaterialTheme.colorScheme.errorContainer
    val errFg = MaterialTheme.colorScheme.onErrorContainer
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(errBg)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.ErrorOutline, null, tint = errFg)
        Spacer(Modifier.width(10.dp))
        Text(message, color = errFg, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (onRetry != null) {
            TextButton(onClick = onRetry) {
                Icon(Icons.Filled.Refresh, null)
                Spacer(Modifier.width(6.dp))
                Text(actionLabel)
            }
        }
        if (onDismiss != null) {
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
fun ErrorBanner(error: AppError, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) =
    ErrorBanner(message = error.message, modifier = modifier, onRetry = onRetry)
