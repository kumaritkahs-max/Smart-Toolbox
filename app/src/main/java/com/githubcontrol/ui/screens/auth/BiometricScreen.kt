package com.githubcontrol.ui.screens.auth

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.githubcontrol.viewmodel.MainViewModel

@Composable
fun BiometricScreen(vm: MainViewModel, onUnlocked: () -> Unit) {
    val ctx = LocalContext.current
    val activity = ctx as? FragmentActivity
    val state by vm.state.collectAsState()
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold { pad ->
        Column(
            Modifier.padding(pad).padding(24.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Welcome back, ${state.activeLogin ?: ""}", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text("Authenticate to unlock", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            Button(onClick = {
                val bm = BiometricManager.from(ctx)
                val can = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                if (can != BiometricManager.BIOMETRIC_SUCCESS || activity == null) {
                    vm.unlock(); onUnlocked(); return@Button
                }
                val executor = ContextCompat.getMainExecutor(ctx)
                val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        vm.unlock(); onUnlocked()
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        error = errString.toString()
                    }
                })
                val info = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock GitHub Control")
                    .setSubtitle("Authenticate to continue")
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                    .build()
                prompt.authenticate(info)
            }, modifier = Modifier.fillMaxWidth()) { Text("Unlock") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { vm.unlock(); onUnlocked() }) { Text("Skip") }
        }
    }
}
