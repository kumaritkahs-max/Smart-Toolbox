package com.githubcontrol.ui.screens.about

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.githubcontrol.R
import com.githubcontrol.ui.components.GhBadge
import com.githubcontrol.ui.components.GhCard
import com.githubcontrol.utils.BuildInfo
import com.githubcontrol.utils.UpdateChecker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, onCheckUpdates: () -> Unit, onClearCache: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var versionTaps by remember { mutableStateOf(0) }
    var checking by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Identity
            GhCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_splash_logo),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                    )
                    Column(Modifier.weight(1f)) {
                        Text("GitHub Control", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Full GitHub management & automation from your phone",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.clickable { versionTaps++ },
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("v${BuildInfo.VERSION_NAME}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (versionTaps >= 5) GhBadge("debug")
                        }
                    }
                }
            }

            // 2. Description
            SectionCard(icon = Icons.Filled.Info, title = "About App") {
                Text(
                    "GitHub Control is a powerful Android client that lets you manage repositories, " +
                    "upload files and folders with full structure, and perform advanced Git operations directly " +
                    "from your device.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // 3. Limitations
            SectionCard(icon = Icons.Filled.Warning, title = "Limitations", iconTint = Color(0xFFD29922)) {
                Bullet("GitHub file size limit (~100 MB per file via API).")
                Bullet("Performance may vary on very large repositories.")
                Bullet("Some features depend on GitHub API rate limits.")
                Bullet("Background tasks need battery-optimisation exemption to run reliably.")
            }

            // 4. Developer
            SectionCard(icon = Icons.Filled.Person, title = "Developer") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(BuildInfo.DEVELOPER_NAME, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Android & Kotlin engineer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { openUrl(ctx, "https://github.com/${BuildInfo.GITHUB_OWNER}") }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null)
                        Spacer(Modifier.width(6.dp))
                        Text("GitHub")
                    }
                    OutlinedButton(onClick = { openEmail(ctx, BuildInfo.DEVELOPER_EMAIL) }) {
                        Icon(Icons.Filled.Email, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Contact Developer")
                    }
                }
            }

            // 5. Version
            SectionCard(icon = Icons.Filled.Verified, title = "Version") {
                Bullet("Version: ${BuildInfo.VERSION_NAME}")
                Bullet("Build: ${BuildInfo.VERSION_CODE}")
                Bullet("Min Android: 8.0 (API 26)")
                Bullet("Target Android: 15 (API 35)")
            }

            // 6. Links
            SectionCard(icon = Icons.AutoMirrored.Filled.OpenInNew, title = "Links") {
                LinkRow("GitHub Repository", BuildInfo.REPO_URL) { openUrl(ctx, BuildInfo.REPO_URL) }
                LinkRow("Report an Issue", BuildInfo.ISSUES_URL) { openUrl(ctx, BuildInfo.ISSUES_URL) }
            }

            // 7. Libraries
            SectionCard(icon = Icons.Filled.Code, title = "Libraries") {
                Bullet("Jetpack Compose · Material 3")
                Bullet("Hilt · KSP · Kotlinx Serialization")
                Bullet("Retrofit · OkHttp")
                Bullet("Room · DataStore · EncryptedSharedPreferences")
                Bullet("JGit · WorkManager")
                Bullet("Coil · BiometricPrompt · DocumentFile")
            }

            // 8. Tech info
            SectionCard(icon = Icons.Filled.Storage, title = "Technical Info") {
                Bullet("API: GitHub REST v2022-11-28")
                Bullet("Git engine: JGit 6.10")
                Bullet("Storage: Internal + SAF")
                Bullet("Build type: ${if (debugBuild()) "Debug" else "Release"}")
            }

            // 9. Quick actions
            SectionCard(icon = Icons.Filled.SystemUpdate, title = "Actions") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            checking = true
                            updateMessage = null
                            scope.launch {
                                val r = UpdateChecker.check()
                                checking = false
                                updateMessage = r.fold(
                                    onSuccess = {
                                        if (it.newer) "Update available: v${it.latest}"
                                        else "You're on the latest version (v${it.current})."
                                    },
                                    onFailure = { e -> "Couldn't check: ${e.message}" }
                                )
                            }
                            onCheckUpdates()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !checking
                    ) {
                        Text(if (checking) "Checking…" else "Check updates")
                    }
                    OutlinedButton(onClick = { openUrl(ctx, BuildInfo.ISSUES_URL) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.BugReport, null); Spacer(Modifier.width(6.dp)); Text("Report")
                    }
                }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = onClearCache, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.CleaningServices, null); Spacer(Modifier.width(6.dp)); Text("Clear cache")
                }
                updateMessage?.let { msg ->
                    Spacer(Modifier.height(6.dp))
                    Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    content: @Composable ColumnScope.() -> Unit
) {
    GhCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = iconTint)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
    }
}

@Composable
private fun Bullet(text: String) {
    Row { Text("• ", color = MaterialTheme.colorScheme.onSurfaceVariant); Text(text) }
}

@Composable
private fun LinkRow(title: String, url: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun openUrl(ctx: Context, url: String) {
    runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure { Toast.makeText(ctx, "Couldn't open $url", Toast.LENGTH_SHORT).show() }
}

private fun openEmail(ctx: Context, email: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
        .putExtra(Intent.EXTRA_SUBJECT, "GitHub Control feedback")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        ctx.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("email", email))
        Toast.makeText(ctx, "No email app installed. Address copied.", Toast.LENGTH_LONG).show()
    }
}

private fun debugBuild(): Boolean = runCatching {
    val cls = Class.forName("com.githubcontrol.BuildConfig")
    cls.getField("DEBUG").getBoolean(null)
}.getOrDefault(false)
