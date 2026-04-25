package com.githubcontrol.ui.screens.files

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.githubcontrol.ui.components.LoadingIndicator
import com.githubcontrol.ui.navigation.Routes
import com.githubcontrol.viewmodel.TreeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreeScreen(owner: String, name: String, ref: String, onBack: () -> Unit, onPreview: (String) -> Unit, vm: TreeViewModel = hiltViewModel()) {
    LaunchedEffect(owner, name, ref) { vm.load(owner, name, ref) }
    val s by vm.state.collectAsState()
    Scaffold(topBar = {
        TopAppBar(title = { Text("Tree") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } })
    }) { pad ->
        if (s.loading) { LoadingIndicator(); return@Scaffold }
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
        if (s.truncated) Text("Tree truncated", color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(8.dp))
        val visible = s.items.filter { it.visible }
        LazyColumn(Modifier.padding(pad)) {
            items(visible, key = { it.path }) { node ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (node.type == "tree") vm.toggle(node.path)
                            else onPreview(Routes.preview(owner, name, node.path, ref))
                        }
                        .padding(horizontal = (12 + node.depth * 16).dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (node.type == "tree") Icon(if (node.expanded) Icons.Filled.ExpandMore else Icons.Filled.ChevronRight, null)
                    else Spacer(Modifier.width(24.dp))
                    Icon(if (node.type == "tree") Icons.Filled.Folder else Icons.Filled.InsertDriveFile, null,
                        tint = if (node.type == "tree") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Text(node.name, modifier = Modifier.weight(1f))
                    if (node.type == "blob") Text(com.githubcontrol.utils.ByteFormat.human(node.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Divider()
            }
        }
    }
}
