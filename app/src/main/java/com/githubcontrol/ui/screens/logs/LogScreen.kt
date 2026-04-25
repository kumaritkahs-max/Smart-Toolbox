package com.githubcontrol.ui.screens.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.githubcontrol.utils.Logger
import com.githubcontrol.utils.ShareUtils

private fun colorFor(level: Logger.Level): Color = when (level) {
    Logger.Level.E -> Color(0xFFF85149)
    Logger.Level.W -> Color(0xFFF0883E)
    Logger.Level.N -> Color(0xFF58A6FF)
    Logger.Level.I -> Color(0xFFE6EDF3)
    Logger.Level.D -> Color(0xFF8B949E)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(onBack: () -> Unit) {
    val all by Logger.entries.collectAsState()
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var paused by remember { mutableStateOf(false) }
    var levelFilter by remember { mutableStateOf<Logger.Level?>(null) }
    val frozen = remember { mutableStateOf<List<Logger.Entry>>(emptyList()) }

    val source = if (paused) frozen.value else all
    val visible = remember(source, query, levelFilter) {
        source.filter { e ->
            (levelFilter == null || e.level == levelFilter) &&
                (query.isBlank() ||
                    e.tag.contains(query, ignoreCase = true) ||
                    e.message.contains(query, ignoreCase = true))
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(visible.size, paused) {
        if (!paused && visible.isNotEmpty()) listState.scrollToItem(visible.size - 1)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Terminal log") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
            actions = {
                IconButton(onClick = {
                    if (!paused) frozen.value = all
                    paused = !paused
                }) {
                    Icon(if (paused) Icons.Filled.PlayCircle else Icons.Filled.PauseCircle, null)
                }
                IconButton(onClick = {
                    ShareUtils.copyToClipboard(ctx, visible.joinToString("\n") { it.formatted() }, "App logs")
                }) { Icon(Icons.Filled.ContentCopy, null) }
                IconButton(onClick = {
                    ShareUtils.shareText(ctx, visible.joinToString("\n") { it.formatted() }, "GitHub Control logs")
                }) { Icon(Icons.Filled.Share, null) }
                IconButton(onClick = { Logger.clear() }) { Icon(Icons.Filled.DeleteSweep, null) }
            }
        )
    }) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().padding(8.dp)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("Filter…") }
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(levelFilter == null, { levelFilter = null }, label = { Text("ALL") })
                Logger.Level.entries.forEach { lvl ->
                    FilterChip(levelFilter == lvl, { levelFilter = lvl }, label = { Text(lvl.tag) })
                }
                Spacer(Modifier.weight(1f))
                Text("${visible.size}", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A0F17))
                    .padding(8.dp)
            ) {
                if (visible.isEmpty()) item {
                    Text(
                        "(no log entries match)",
                        color = Color(0xFF8B949E),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
                items(visible, key = { it.id }) { e ->
                    Text(e.formatted(), color = colorFor(e.level), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (paused) "Paused — buffer frozen at ${frozen.value.size} entries"
                else "Live · 1500-entry rolling buffer · secrets redacted",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
