package com.githubcontrol.ui.screens.repos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.githubcontrol.viewmodel.CreateRepoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRepoScreen(onBack: () -> Unit, onCreated: (String, String) -> Unit, vm: CreateRepoViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    LaunchedEffect(s.created) { s.created?.let { onCreated(it.owner.login, it.name) } }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("New repository") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } })
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(s.name, { vm.update(s.copy(name = it)) }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.description, { vm.update(s.copy(description = it)) }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = s.private, onCheckedChange = { vm.update(s.copy(private = it)) })
                Text("Private repository", modifier = Modifier.padding(start = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = s.autoInit, onCheckedChange = { vm.update(s.copy(autoInit = it)) })
                Text("Initialize with README", modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedTextField(s.gitignore, { vm.update(s.copy(gitignore = it)) }, label = { Text(".gitignore template (e.g. Node, Python)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.license, { vm.update(s.copy(license = it)) }, label = { Text("License (e.g. mit, apache-2.0)") }, modifier = Modifier.fillMaxWidth())
            Divider()
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(s.hasIssues, { vm.update(s.copy(hasIssues = it)) }); Text("Issues", Modifier.padding(start = 8.dp)) }
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(s.hasWiki, { vm.update(s.copy(hasWiki = it)) }); Text("Wiki", Modifier.padding(start = 8.dp)) }
            Row(verticalAlignment = Alignment.CenterVertically) { Switch(s.hasProjects, { vm.update(s.copy(hasProjects = it)) }); Text("Projects", Modifier.padding(start = 8.dp)) }
            if (s.error != null) Text(s.error!!, color = MaterialTheme.colorScheme.error)
            Button(onClick = { vm.submit() }, enabled = !s.busy && s.name.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                if (s.busy) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp)) else Text("Create repository")
            }
        }
    }
}
