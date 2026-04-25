package com.githubcontrol.ui.screens.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.githubcontrol.ui.components.EmbeddedTerminal
import com.githubcontrol.ui.components.GhBadge
import com.githubcontrol.ui.components.GhCard
import com.githubcontrol.upload.ConflictMode
import com.githubcontrol.upload.UploadFileState
import com.githubcontrol.utils.ByteFormat
import com.githubcontrol.viewmodel.UploadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(owner: String, name: String, path: String, ref: String, onBack: () -> Unit, vm: UploadViewModel = hiltViewModel()) {
    LaunchedEffect(owner, name, path, ref) { vm.init(owner, name, path, ref) }
    val form by vm.form.collectAsState()
    val progress by vm.progress.collectAsState()

    val pickFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> if (uris.isNotEmpty()) vm.setUris(uris) }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if (uri != null) vm.setUris(listOf(uri)) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Upload to $name") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GhCard {
                Text("Destination", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(form.targetFolder, { vm.setTarget(it) }, label = { Text("Folder path inside repo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(form.branch, { vm.setBranch(it) }, label = { Text("Branch") }, singleLine = true, modifier = Modifier.fillMaxWidth(), supportingText = { Text("Available: ${form.branches.joinToString(", ")}") })
            }

            GhCard {
                Text("Pick files or folders", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickFiles.launch(arrayOf("*/*")) }) { Icon(Icons.Filled.AttachFile, null); Spacer(Modifier.width(6.dp)); Text("Files") }
                    OutlinedButton(onClick = { pickFolder.launch(null) }) { Icon(Icons.Filled.FolderOpen, null); Spacer(Modifier.width(6.dp)); Text("Folder") }
                }
                if (form.totalFiles > 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GhBadge("${form.totalFiles} files")
                        GhBadge(ByteFormat.human(form.totalBytes))
                    }
                }
            }

            GhCard {
                Text("Commit", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(form.message, { vm.setMessage(it) }, label = { Text("Commit message") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { vm.aiSuggestMessage() }) { Icon(Icons.Filled.AutoAwesome, null); Spacer(Modifier.width(4.dp)); Text("AI suggest") }
                Divider()
                Text("On conflict", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ConflictMode.values().forEach { m ->
                        FilterChip(selected = form.conflictMode == m, onClick = { vm.setMode(m) }, label = { Text(m.name) })
                    }
                }
                Divider()
                OutlinedTextField(form.authorName ?: "", { vm.setAuthor(it, form.authorEmail) }, label = { Text("Author name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(form.authorEmail ?: "", { vm.setAuthor(form.authorName, it) }, label = { Text("Author email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Divider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(form.dryRun, { vm.setDryRun(it) })
                    Spacer(Modifier.width(8.dp))
                    Column { Text("Dry run", style = MaterialTheme.typography.bodyMedium); Text("Preview without committing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }

            if (progress.running || progress.finished) {
                GhCard {
                    Text(if (progress.finished) "Finished" else "Uploading…", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { if (form.totalFiles == 0) 0f else progress.uploaded / form.totalFiles.toFloat() },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${progress.uploaded}/${progress.total}  ${ByteFormat.human(progress.bytesDone)} @ ${ByteFormat.human(progress.bytesPerSec.toLong())}/s  ETA ${progress.etaSeconds}s")
                    if (progress.currentFile.isNotEmpty()) Text(progress.currentFile, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (progress.running && !progress.paused) OutlinedButton(onClick = { vm.pause() }) { Text("Pause") }
                        if (progress.running && progress.paused) OutlinedButton(onClick = { vm.resume() }) { Text("Resume") }
                        if (progress.running) OutlinedButton(onClick = { vm.cancel() }) { Text("Cancel") }
                    }
                }
            }
            Button(onClick = { vm.start() }, enabled = form.totalFiles > 0 && !progress.running, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CloudUpload, null); Spacer(Modifier.width(6.dp))
                Text(if (form.dryRun) "Run preview" else "Start upload")
            }
            EmbeddedTerminal(section = "Upload", initiallyExpanded = progress.running)
            if (progress.files.isNotEmpty()) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                    items(progress.files, key = { it.id }) { uf ->
                        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            val icon = when (uf.state) {
                                UploadFileState.DONE -> Icons.Filled.CheckCircle
                                UploadFileState.FAILED -> Icons.Filled.Error
                                UploadFileState.SKIPPED -> Icons.Filled.Block
                                UploadFileState.UPLOADING -> Icons.Filled.CloudUpload
                                else -> Icons.Filled.Pending
                            }
                            Icon(icon, null, tint = when (uf.state) {
                                UploadFileState.DONE -> MaterialTheme.colorScheme.primary
                                UploadFileState.FAILED -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            })
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(uf.targetPath, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                if (uf.error != null) Text(uf.error!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                            Text(ByteFormat.human(uf.sizeBytes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
