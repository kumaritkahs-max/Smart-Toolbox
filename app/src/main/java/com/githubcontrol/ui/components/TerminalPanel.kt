package com.githubcontrol.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.githubcontrol.utils.Logger
import com.githubcontrol.utils.ShareUtils
import kotlinx.coroutines.flow.collect

/**
 * Modern collapsible terminal log panel. Drop on any screen with [section] set to
 * a short tag (e.g. "Upload", "Sync"). Filters its view to entries with that tag
 * (or matching the section keyword) but still has a "Show all" mode.
 */
@Composable
fun EmbeddedTerminal(
    section: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    maxHeight: androidx.compose.ui.unit.Dp = 220.dp
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    var showAll by remember { mutableStateOf(false) }
    var levelFilter by remember { mutableStateOf<Logger.Level?>(null) }
    val all by Logger.entries.collectAsState()
    val ctx = LocalContext.current

    val visible = remember(all, section, showAll, levelFilter) {
        val baseScope = if (showAll) all else all.filter {
            it.tag.equals(section, ignoreCase = true) ||
                it.tag.contains(section, ignoreCase = true) ||
                it.message.contains(section, ignoreCase = true)
        }
        if (levelFilter == null) baseScope else baseScope.filter { it.level == levelFilter }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(visible.size, expanded) {
        if (expanded && visible.isNotEmpty()) listState.scrollToItem(visible.size - 1)
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .background(Color(0xFF0A0F17))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Terminal, null, tint = Color(0xFF58A6FF))
            Spacer(Modifier.width(8.dp))
            Text(
                "Terminal · $section",
                color = Color(0xFFE6EDF3),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF1F6FEB).copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("${visible.size}", color = Color(0xFF58A6FF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                ShareUtils.copyToClipboard(ctx, Logger.snapshot().joinToString("\n") { it.formatted() }.ifBlank { "(empty log)" }, "App logs")
            }) { Icon(Icons.Filled.ContentCopy, "Copy logs", tint = Color(0xFF8B949E)) }
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = Color(0xFF8B949E))
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = !showAll,
                        onClick = { showAll = false },
                        label = { Text("Section", fontSize = 11.sp) }
                    )
                    Spacer(Modifier.width(6.dp))
                    FilterChip(
                        selected = showAll,
                        onClick = { showAll = true },
                        label = { Text("All", fontSize = 11.sp) }
                    )
                    Spacer(Modifier.width(8.dp))
                    listOf<Logger.Level?>(null, Logger.Level.I, Logger.Level.W, Logger.Level.E, Logger.Level.N).forEach { lvl ->
                        AssistChip(
                            onClick = { levelFilter = lvl },
                            label = { Text(lvl?.tag ?: "ALL", fontSize = 10.sp) },
                            colors = if (levelFilter == lvl)
                                AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1F6FEB).copy(alpha = 0.25f))
                            else AssistChipDefaults.assistChipColors()
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { Logger.clear() }) {
                        Icon(Icons.Filled.DeleteSweep, "Clear", tint = Color(0xFF8B949E))
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = maxHeight)
                        .padding(8.dp)
                ) {
                    if (visible.isEmpty()) {
                        item { Text("(no logs yet)", color = Color(0xFF8B949E), fontSize = 12.sp, fontFamily = FontFamily.Monospace) }
                    } else {
                        items(visible, key = { it.id }) { e ->
                            Text(
                                e.formatted(),
                                color = colorFor(e.level),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun colorFor(level: Logger.Level): Color = when (level) {
    Logger.Level.E -> Color(0xFFF85149)
    Logger.Level.W -> Color(0xFFF0883E)
    Logger.Level.N -> Color(0xFF58A6FF)
    Logger.Level.I -> Color(0xFFE6EDF3)
    Logger.Level.D -> Color(0xFF8B949E)
}
