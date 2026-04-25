package com.githubcontrol.plugins

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class Plugin(val id: String, val name: String, val description: String, val enabled: Boolean = true, val builtIn: Boolean = true)

@Singleton
class PluginRegistry @Inject constructor() {
    val plugins = MutableStateFlow(
        listOf(
            Plugin("ai-commit", "AI commit messages", "Suggest commit messages from staged files."),
            Plugin("dry-run", "Dry-run preview", "Preview every change before committing."),
            Plugin("backup", "Repo backup", "Snapshot repos to local storage."),
            Plugin("zip-import", "ZIP importer", "Expand ZIP archives during upload."),
            Plugin("background-uploads", "Background uploads", "Resume uploads when the app is closed."),
        )
    )

    fun toggle(id: String) {
        plugins.value = plugins.value.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }
    }
}
