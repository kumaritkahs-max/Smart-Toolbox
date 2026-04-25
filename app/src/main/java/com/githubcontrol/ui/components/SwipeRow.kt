package com.githubcontrol.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeRow(
    onDelete: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    leftLabel: String = "Archive",
    rightLabel: String = "Delete",
    leftColor: Color = MaterialTheme.colorScheme.tertiary,
    rightColor: Color = MaterialTheme.colorScheme.error,
    content: @Composable () -> Unit
) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.EndToStart -> { onDelete?.invoke(); false }
                SwipeToDismissBoxValue.StartToEnd -> { onArchive?.invoke(); false }
                else -> false
            }
        }
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            val bg = when (state.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> rightColor
                SwipeToDismissBoxValue.StartToEnd -> leftColor
                else -> Color.Transparent
            }
            val label = when (state.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> rightLabel
                SwipeToDismissBoxValue.StartToEnd -> leftLabel
                else -> ""
            }
            Box(
                Modifier.fillMaxSize().background(bg).padding(horizontal = 20.dp),
                contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Text(label, color = Color.White)
            }
        },
        content = { content() }
    )
}
