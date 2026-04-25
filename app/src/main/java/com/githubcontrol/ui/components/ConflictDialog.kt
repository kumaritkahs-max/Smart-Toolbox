package com.githubcontrol.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

enum class ConflictChoice { KEEP_LOCAL, KEEP_REMOTE, MANUAL }

/** Lightweight "merge editor" — shows local + remote side by side and lets the user pick or hand-edit. */
@Composable
fun ConflictDialog(
    path: String,
    local: String,
    remote: String,
    onResolved: (ConflictChoice, String) -> Unit,
    onDismiss: () -> Unit
) {
    var manual by remember { mutableStateOf(local) }
    var mode by remember { mutableStateOf(ConflictChoice.KEEP_LOCAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conflict in $path") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(mode == ConflictChoice.KEEP_LOCAL, { mode = ConflictChoice.KEEP_LOCAL }, label = { Text("Keep local") })
                    FilterChip(mode == ConflictChoice.KEEP_REMOTE, { mode = ConflictChoice.KEEP_REMOTE }, label = { Text("Keep remote") })
                    FilterChip(mode == ConflictChoice.MANUAL, { mode = ConflictChoice.MANUAL }, label = { Text("Manual") })
                }
                Spacer(Modifier.height(8.dp))
                when (mode) {
                    ConflictChoice.KEEP_LOCAL -> Text(local, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    ConflictChoice.KEEP_REMOTE -> Text(remote, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    ConflictChoice.MANUAL -> OutlinedTextField(manual, { manual = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val text = when (mode) {
                    ConflictChoice.KEEP_LOCAL -> local
                    ConflictChoice.KEEP_REMOTE -> remote
                    ConflictChoice.MANUAL -> manual
                }
                onResolved(mode, text)
            }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
