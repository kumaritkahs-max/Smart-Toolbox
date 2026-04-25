package com.githubcontrol.ui.screens.issues

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
import com.githubcontrol.data.api.CreateIssueRequest
import com.githubcontrol.viewmodel.IssuesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateIssueScreen(owner: String, name: String, onBack: () -> Unit, onCreated: (Int) -> Unit, vm: IssuesViewModel = hiltViewModel()) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var labels by remember { mutableStateOf("") }
    var assignees by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("New issue") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(body, { body = it }, label = { Text("Description (Markdown)") }, modifier = Modifier.fillMaxWidth().height(180.dp))
            OutlinedTextField(labels, { labels = it }, label = { Text("Labels (comma-separated)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(assignees, { assignees = it }, label = { Text("Assignees (comma-separated logins)") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                vm.create(owner, name, CreateIssueRequest(
                    title = title, body = body.ifBlank { null },
                    labels = labels.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                    assignees = assignees.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                )) { i -> i?.number?.let(onCreated) }
            }, enabled = title.isNotBlank()) { Text("Create issue") }
        }
    }
}
