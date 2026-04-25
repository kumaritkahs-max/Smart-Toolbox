package com.githubcontrol.ui.screens.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.githubcontrol.ui.components.GhCard
import com.githubcontrol.ui.components.StatPill
import com.githubcontrol.upload.UploadFileState
import com.githubcontrol.upload.UploadManager
import com.githubcontrol.utils.Logger
import com.githubcontrol.utils.Recovery

/**
 * One-glance status dashboard: API rate, last sync, pending uploads, recent
 * errors, last activity. Reads from in-memory state plus the recovery snapshot
 * so it works even after a crash.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    main: com.githubcontrol.viewmodel.MainViewModel,
    uploadManager: UploadManager,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val rate by main.accountManager.rateRemaining.collectAsState()
    val state by main.state.collectAsState()
    val upload by uploadManager.state.collectAsState()
    val logs by Logger.entries.collectAsState()
    var refreshTick by remember { mutableStateOf(0) }
    val errorCount = remember(logs) { logs.count { it.level == Logger.Level.E } }
    val recentErrors = remember(logs) {
        logs.filter { it.level == Logger.Level.E }.takeLast(5).reversed()
    }
    val pendingUpload = remember(upload, refreshTick) { Recovery.pendingUpload(ctx) }
    val lastActivity = remember(refreshTick) { Recovery.lastSessionAt(ctx) }
    val failed = remember(upload) { upload.files.count { it.state == UploadFileState.FAILED } }
    val skipped = remember(upload) { upload.files.count { it.state == UploadFileState.SKIPPED } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health & Status") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { refreshTick++ }) { Icon(Icons.Filled.Refresh, null) }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // GitHub API
            GhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val ok = rate != null && (rate ?: 0) > 100
                    Icon(
                        if (ok) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                        null,
                        tint = if (ok) Color(0xFF3FB950) else Color(0xFFD29922)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("GitHub API", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            rate?.let { "$it requests left this hour" }
                                ?: "Rate limit unknown — make a request to refresh.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Uploads
            GhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Sync, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Uploads", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatPill("done", upload.uploaded.toString())
                    StatPill("failed", failed.toString())
                    StatPill("skipped", skipped.toString())
                    StatPill("total", upload.total.toString())
                }
                if (upload.currentFile.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Currently: ${upload.currentFile}", style = MaterialTheme.typography.bodySmall)
                }
                if (pendingUpload != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Resumable upload: ${pendingUpload.completedFiles}/${pendingUpload.totalFiles} → " +
                            "${pendingUpload.owner}/${pendingUpload.repo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = {
                            Recovery.clearPendingUpload(ctx); refreshTick++
                        }) { Text("Discard") }
                    }
                }
            }

            // Errors
            GhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.ErrorOutline, null,
                        tint = if (errorCount > 0) Color(0xFFF85149) else Color(0xFF3FB950)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Errors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("$errorCount", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                if (recentErrors.isEmpty()) {
                    Text(
                        "No errors in this session.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    recentErrors.forEach { e ->
                        Text("• ${e.tag}: ${e.message}",
                            style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
            }

            // Last activity
            GhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.HourglassBottom, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Last activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val human = if (lastActivity > 0) {
                            val secs = (System.currentTimeMillis() - lastActivity) / 1000
                            when {
                                secs < 60 -> "${secs}s ago"
                                secs < 3600 -> "${secs / 60}m ago"
                                secs < 86400 -> "${secs / 3600}h ago"
                                else -> "${secs / 86400}d ago"
                            }
                        } else "—"
                        Text(human, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Account
            GhCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
                Text("Active: ${state.activeLogin ?: "(none)"}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Signed in: ${state.loggedIn} · Locked: ${state.locked}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
