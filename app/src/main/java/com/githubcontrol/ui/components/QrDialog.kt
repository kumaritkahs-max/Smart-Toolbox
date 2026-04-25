package com.githubcontrol.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.githubcontrol.utils.ShareUtils

@Composable
fun QrDialog(text: String, title: String = "QR code", onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val bmp = remember(text) { ShareUtils.qrBitmap(text, 600) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(bitmap = bmp.asImageBitmap(), contentDescription = "QR code", modifier = Modifier.size(260.dp))
                Spacer(Modifier.height(8.dp))
                Text(text, style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { ShareUtils.copyToClipboard(ctx, text); onDismiss() }) { Text("Copy URL") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
