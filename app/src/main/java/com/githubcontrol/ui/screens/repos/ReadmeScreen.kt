package com.githubcontrol.ui.screens.repos

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.githubcontrol.ui.components.LoadingIndicator
import com.githubcontrol.viewmodel.ReadmeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ReadmeScreen(owner: String, name: String, ref: String?, onBack: () -> Unit, vm: ReadmeViewModel = hiltViewModel()) {
    LaunchedEffect(owner, name, ref) { vm.load(owner, name, ref) }
    val s by vm.state.collectAsState()
    val ctx = LocalContext.current

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("$owner/$name • README") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
            actions = { IconButton(onClick = { vm.load(owner, name, ref) }) { Icon(Icons.Filled.Refresh, null) } }
        )
    }) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                s.loading -> LoadingIndicator()
                s.error != null -> Text(
                    "Could not render README: ${s.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                s.html != null -> {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            WebView(it).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadsImagesAutomatically = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                webViewClient = WebViewClient()
                            }
                        },
                        update = { wv ->
                            wv.loadDataWithBaseURL("https://github.com/", s.html!!, "text/html", "UTF-8", null)
                        }
                    )
                }
            }
        }
    }
}
