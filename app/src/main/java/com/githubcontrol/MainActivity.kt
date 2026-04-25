package com.githubcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.githubcontrol.data.auth.AccountManager
import com.githubcontrol.ui.AppRoot
import com.githubcontrol.ui.theme.GitHubControlTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var accountManager: AccountManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val theme = accountManager.themeFlow.collectAsState(initial = "system")
            GitHubControlTheme(themeMode = theme.value) {
                AppRoot()
            }
        }
    }
}
