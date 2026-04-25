package com.githubcontrol.ui.screens.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.githubcontrol.ui.components.GhBadge
import com.githubcontrol.ui.components.GhCard
import com.githubcontrol.ui.theme.AccentPalette
import com.githubcontrol.ui.theme.TerminalPalette
import com.githubcontrol.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
@Composable
fun AppearanceScreen(main: MainViewModel, onBack: () -> Unit) {
    val am = main.accountManager
    val scope = rememberCoroutineScope()
    val mode by am.themeFlow.collectAsState(initial = "system")
    val accent by am.accentColorFlow.collectAsState(initial = "blue")
    val dynamic by am.dynamicColorFlow.collectAsState(initial = false)
    val amoled by am.amoledFlow.collectAsState(initial = false)
    val fontScale by am.fontScaleFlow.collectAsState(initial = 1.0f)
    val monoScale by am.monoFontScaleFlow.collectAsState(initial = 1.0f)
    val density by am.densityFlow.collectAsState(initial = "comfortable")
    val corner by am.cornerRadiusFlow.collectAsState(initial = 14)
    val terminal by am.terminalThemeFlow.collectAsState(initial = "github-dark")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    TextButton(onClick = { scope.launch { am.resetAppearance() } }) { Text("Reset") }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // -- Live preview card
            GhCard {
                Text("Preview", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GhBadge("primary")
                    GhBadge("ok", color = MaterialTheme.colorScheme.tertiary)
                    GhBadge("danger", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}) { Text("Button") }
                    OutlinedButton(onClick = {}) { Text("Outline") }
                    TextButton(onClick = {}) { Text("Text") }
                }
            }

            // -- Mode
            GhCard {
                SectionHeader("Mode")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("system", "light", "dark").forEach {
                        FilterChip(selected = mode == it, onClick = { scope.launch { am.setTheme(it) } }, label = { Text(it) })
                    }
                }
                if (mode == "dark" || (mode == "system" && isSystemInDark())) {
                    Spacer(Modifier.height(6.dp))
                    SwitchRow("Pure black (AMOLED)", amoled) { scope.launch { am.setAmoled(it) } }
                }
            }

            // -- Accent palette
            GhCard {
                SectionHeader("Accent color")
                if (dynamic) {
                    Text(
                        "Material You is on — accent color is taken from your system wallpaper.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowAccent(accent) { key -> scope.launch { am.setAccent(key) } }
                }
                Spacer(Modifier.height(6.dp))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchRow("Use Material You (system wallpaper colors)", dynamic) { scope.launch { am.setDynamicColor(it) } }
                } else {
                    Text(
                        "Material You needs Android 12 or newer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // -- Density / spacing
            GhCard {
                SectionHeader("Density")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("compact", "comfortable", "cozy").forEach {
                        FilterChip(selected = density == it, onClick = { scope.launch { am.setDensity(it) } }, label = { Text(it) })
                    }
                }
            }

            // -- Corner radius
            GhCard {
                SectionHeader("Corner radius — ${corner}dp")
                Slider(
                    value = corner.toFloat(),
                    onValueChange = { scope.launch { am.setCornerRadius(it.toInt()) } },
                    valueRange = 0f..28f,
                    steps = 27
                )
            }

            // -- Font scale
            GhCard {
                SectionHeader("Text size — ${"%.2f".format(fontScale)}×")
                Slider(
                    value = fontScale,
                    onValueChange = { scope.launch { am.setFontScale(it) } },
                    valueRange = 0.7f..1.6f, steps = 17
                )
                Spacer(Modifier.height(8.dp))
                SectionHeader("Code / monospace size — ${"%.2f".format(monoScale)}×")
                Slider(
                    value = monoScale,
                    onValueChange = { scope.launch { am.setMonoFontScale(it) } },
                    valueRange = 0.7f..1.6f, steps = 17
                )
            }

            // -- Terminal palette
            GhCard {
                SectionHeader("Terminal & code-block palette")
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TerminalPalette.all.forEach { t ->
                        AssistChip(
                            onClick = { scope.launch { am.setTerminalTheme(t.key) } },
                            leadingIcon = {
                                Box(
                                    Modifier.size(14.dp).clip(CircleShape)
                                        .background(t.bg)
                                        .border(1.dp, t.fg, CircleShape)
                                )
                            },
                            label = { Text(t.label) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (terminal == t.key) MaterialTheme.colorScheme.primaryContainer
                                                 else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val tp = TerminalPalette.byKey(terminal)
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(tp.bg).padding(10.dp)
                ) {
                    Text(
                        "$ git push origin main\n→ pushed 3 commits, 5 files\n",
                        color = tp.fg, fontFamily = FontFamily.Monospace, fontSize = (12 * monoScale).sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SwitchRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowAccent(currentKey: String, onPick: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AccentPalette.all.forEach { sw ->
            val selected = sw.key == currentKey
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(sw.color)
                    .border(2.dp, if (selected) Color.White else Color.Transparent, CircleShape)
                    .clickable { onPick(sw.key) },
                contentAlignment = Alignment.Center
            ) {
                if (selected) Icon(Icons.Filled.Check, null, tint = Color.White)
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}

@Composable
private fun isSystemInDark(): Boolean = androidx.compose.foundation.isSystemInDarkTheme()
