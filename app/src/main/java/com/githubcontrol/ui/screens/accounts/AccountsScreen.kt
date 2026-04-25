package com.githubcontrol.ui.screens.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.githubcontrol.ui.components.GhCard
import com.githubcontrol.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(vm: MainViewModel, onBack: () -> Unit, onAdd: () -> Unit) {
    val s by vm.state.collectAsState()
    Scaffold(topBar = {
        TopAppBar(title = { Text("Accounts") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
            actions = { IconButton(onClick = onAdd) { Icon(Icons.Filled.Add, null) } })
    }) { pad ->
        LazyColumn(Modifier.padding(pad).fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(s.accounts, key = { it.id }) { acc ->
                GhCard(onClick = { vm.switchAccount(acc.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = acc.avatarUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(acc.name ?: acc.login, fontWeight = FontWeight.SemiBold)
                            Text("@${acc.login}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (acc.id == s.activeLogin) AssistChip(onClick = {}, label = { Text("Active") })
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { vm.logoutActive() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Logout, null); Spacer(Modifier.width(8.dp)); Text("Sign out current account")
                }
            }
        }
    }
}
