package com.githubcontrol.ui.screens.pulls

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.githubcontrol.data.api.CreatePRRequest
import com.githubcontrol.viewmodel.PullsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePullScreen(owner: String, name: String, onBack: () -> Unit, onCreated: (Int) -> Unit, vm: PullsViewModel = hiltViewModel()) {
    var title by remember { mutableStateOf("") }
    var head by remember { mutableStateOf("") }
    var base by remember { mutableStateOf("main") }
    var body by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Open pull request") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(head, { head = it }, label = { Text("Head branch") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(base, { base = it }, label = { Text("Base branch") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(body, { body = it }, label = { Text("Description (Markdown)") }, modifier = Modifier.fillMaxWidth().height(160.dp))
            Row { Switch(draft, { draft = it }); Text("Draft", modifier = Modifier.padding(start = 8.dp, top = 12.dp)) }
            Button(onClick = {
                busy = true
                vm.create(owner, name, CreatePRRequest(title, head, base, body, draft)) { pr -> busy = false; pr?.number?.let(onCreated) }
            }, enabled = !busy && title.isNotBlank() && head.isNotBlank() && base.isNotBlank()) { Text("Create") }
        }
    }
}
