package com.githubcontrol

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.githubcontrol.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint

// FragmentActivity (which extends ComponentActivity) is required so that
// androidx.biometric.BiometricPrompt can attach its dialog fragment for the
// unlock flow used by [BiometricScreen]. Plain ComponentActivity silently
// falls through to "auto-unlock", defeating the security gate.
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The whole app theme — including all Settings → Appearance customizations —
        // is composed inside [AppRoot] so it can react to live preference changes.
        setContent { AppRoot() }
    }
}
