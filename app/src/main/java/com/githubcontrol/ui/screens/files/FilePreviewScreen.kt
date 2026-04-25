package com.githubcontrol.ui.screens.files

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.githubcontrol.ui.components.LoadingIndicator
import com.githubcontrol.viewmodel.PreviewViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePreviewScreen(owner: String, name: String, path: String, ref: String, onBack: () -> Unit, vm: PreviewViewModel = hiltViewModel()) {
    LaunchedEffect(owner, name, path, ref) { vm.load(owner, name, path, ref) }
    val s by vm.state.collectAsState()
    var editing by remember { mutableStateOf(false) }
    var content by remember(s.text) { mutableStateOf(s.text ?: "") }
    var commitMsg by remember { mutableStateOf("Update $path") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(path, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (s.text != null && !editing) TextButton(onClick = { editing = true }) { Text("Edit") }
                    if (editing) {
                        IconButton(onClick = {
                            vm.save(owner, name, path, ref, content, commitMsg) { editing = false }
                        }) { Icon(Icons.Filled.Save, null) }
                    }
                }
            )
        }
    ) { pad ->
        if (s.loading) { LoadingIndicator(); return@Scaffold }
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)); return@Scaffold }

        Column(Modifier.padding(pad).fillMaxSize().padding(12.dp)) {
            if (s.isImage && s.content != null) {
                AsyncImage(
                    model = s.content!!.downloadUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                )
            } else if (editing) {
                OutlinedTextField(
                    commitMsg, { commitMsg = it },
                    label = { Text("Commit message") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    content, { content = it },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
            } else if (s.text != null) {
                val isMarkdown = path.endsWith(".md", true) || path.endsWith(".markdown", true)
                if (isMarkdown) {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                            .verticalScroll(rememberScrollState()).padding(12.dp)
                    ) {
                        MarkdownText(markdown = s.text!!, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Box(
                        Modifier.fillMaxSize().background(Color(0xFF0D1117), RoundedCornerShape(12.dp))
                            .verticalScroll(rememberScrollState()).padding(12.dp)
                    ) {
                        Text(s.text!!, color = Color(0xFFE6EDF3), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                }
            } else {
                Text("Binary or unsupported file. Size: ${s.content?.size ?: 0} bytes")
            }
        }
    }
}
