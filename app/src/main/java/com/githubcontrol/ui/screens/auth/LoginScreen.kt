package com.githubcontrol.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.githubcontrol.R
import com.githubcontrol.data.auth.ScopeCatalog
import com.githubcontrol.data.auth.TokenValidator
import com.githubcontrol.ui.components.EmbeddedTerminal
import com.githubcontrol.ui.components.GhBadge
import com.githubcontrol.ui.components.GhCard
import com.githubcontrol.utils.ShareUtils
import com.githubcontrol.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(vm: MainViewModel, onSignedIn: () -> Unit) {
    var token by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val busy by vm.loginBusy.collectAsState()
    val err by vm.loginError.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var preview by remember { mutableStateOf<TokenValidator.Result?>(null) }
    var checking by remember { mutableStateOf(false) }
    val tokenType = remember(token) { if (token.isNotBlank()) TokenValidator.classify(token) else null }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("GitHub Control") }) }) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineMedium)
            Text(
                stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            GhCard {
                OutlinedTextField(
                    value = token, onValueChange = { token = it.trim(); preview = null },
                    label = { Text("Personal Access Token") },
                    placeholder = { Text(stringResource(R.string.pat_hint)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                        }
                    }
                )
                if (tokenType != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GhBadge(
                            tokenType.uppercase(),
                            color = when (tokenType) {
                                "fine-grained" -> MaterialTheme.colorScheme.primary
                                "classic" -> MaterialTheme.colorScheme.tertiary
                                "unknown" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        )
                        Text("${token.length} chars", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                checking = true
                                preview = vm.previewValidate(token)
                                checking = false
                            }
                        },
                        enabled = token.isNotBlank() && !checking,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (checking) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        else Text("Validate")
                    }
                    Button(
                        onClick = { vm.signInWithToken(token) { onSignedIn() } },
                        enabled = !busy && token.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (busy) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        else Text(stringResource(R.string.sign_in))
                    }
                }
            }

            if (err != null) {
                Text(err!!, color = MaterialTheme.colorScheme.error)
            }

            preview?.let { p ->
                GhCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (p.ok) Icons.Filled.CheckCircle else Icons.Filled.Error, null,
                            tint = if (p.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (p.ok) "Token is valid" else "Token rejected",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                            color = if (p.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    if (p.ok) {
                        Spacer(Modifier.height(6.dp))
                        Text("Signed in as @${p.login}  ·  ${p.name ?: "no display name"}")
                        if (p.publicRepos > 0 || p.followers > 0)
                            Text("public repos: ${p.publicRepos}  ·  followers: ${p.followers}",
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val rl = p.validation.rateLimit
                    val rm = p.validation.rateMax
                    val expiry = p.validation.tokenExpiry
                    val type = p.validation.tokenType
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (type != null) GhBadge(type.uppercase())
                        if (rl != null) GhBadge("rate $rl${if (rm != null) "/$rm" else ""}")
                        if (expiry != null) GhBadge("expires $expiry", MaterialTheme.colorScheme.tertiary)
                    }
                    if (p.error != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(p.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (p.scopes.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Granted permissions (${p.scopes.size})", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(4.dp))
                        p.scopes.forEach { sc ->
                            val info = ScopeCatalog.describe(sc)
                            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 3.dp)) {
                                GhBadge(
                                    sc, color = when (info.risk) {
                                        ScopeCatalog.Risk.CRITICAL -> MaterialTheme.colorScheme.error
                                        ScopeCatalog.Risk.HIGH -> MaterialTheme.colorScheme.error
                                        ScopeCatalog.Risk.MEDIUM -> MaterialTheme.colorScheme.tertiary
                                        ScopeCatalog.Risk.LOW -> MaterialTheme.colorScheme.primary
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(info.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(info.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else if (p.ok) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "GitHub did not return any scope headers — this is normal for fine-grained tokens. Permissions can be inspected at github.com/settings/tokens.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val recommended = ScopeCatalog.recommended.filterNot { req -> p.scopes.any { it == req || it.startsWith("$req:") } }
                    if (p.ok && recommended.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(6.dp))
                            Text("Missing recommended scopes:", style = MaterialTheme.typography.labelMedium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            recommended.forEach { GhBadge(it, MaterialTheme.colorScheme.tertiary) }
                        }
                    }
                }
            }

            GhCard {
                Text("Need a token?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Create a Personal Access Token at github.com/settings/tokens. Recommended scopes: " +
                        ScopeCatalog.recommended.joinToString(", ") + ".",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { ShareUtils.openInBrowser(ctx, "https://github.com/settings/tokens/new?scopes=${ScopeCatalog.recommended.joinToString(",")}&description=GitHub%20Control") }) {
                        Icon(Icons.Filled.OpenInBrowser, null); Spacer(Modifier.width(4.dp)); Text("Classic PAT")
                    }
                    OutlinedButton(onClick = { ShareUtils.openInBrowser(ctx, "https://github.com/settings/personal-access-tokens/new") }) {
                        Icon(Icons.Filled.OpenInBrowser, null); Spacer(Modifier.width(4.dp)); Text("Fine-grained")
                    }
                }
            }

            EmbeddedTerminal(section = "TokenValidator")
        }
    }
}
