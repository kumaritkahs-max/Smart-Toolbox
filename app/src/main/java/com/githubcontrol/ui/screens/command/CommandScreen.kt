package com.githubcontrol.ui.screens.command

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.githubcontrol.viewmodel.CommandViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandScreen(onBack: () -> Unit, vm: CommandViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val listState = rememberLazyListState()
    LaunchedEffect(s.lines.size) { if (s.lines.isNotEmpty()) listState.animateScrollToItem(s.lines.size - 1) }
    Scaffold(topBar = { TopAppBar(title = { Text("Command Mode") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }) }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(12.dp)) {
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF0D1117)).padding(8.dp)) {
                if (s.lines.isEmpty()) item {
                    Text("Type 'help' to see commands.", color = Color(0xFF8B949E), fontFamily = FontFamily.Monospace)
                }
                items(s.lines) { line ->
                    Text("$ ${line.cmd}", color = Color(0xFF2F81F7), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text(line.output, color = if (line.ok) Color(0xFFE6EDF3) else Color(0xFFF85149), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(s.input, { vm.setInput(it) }, modifier = Modifier.weight(1f), placeholder = { Text("Type command…") }, singleLine = true)
                IconButton(onClick = { vm.run() }, enabled = !s.running) { Icon(Icons.Filled.PlayArrow, null) }
            }
        }
    }
}
