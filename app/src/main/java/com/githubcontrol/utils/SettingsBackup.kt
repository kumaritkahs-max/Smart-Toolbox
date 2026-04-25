package com.githubcontrol.utils

import android.content.Context
import android.net.Uri
import com.githubcontrol.data.auth.AccountManager
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Settings + accounts export / import. Tokens are NEVER serialized — only the
 * non-secret metadata. After importing, the user has to re-paste their PAT for
 * each account.
 */
object SettingsBackup {

    @Serializable
    data class AccountStub(
        val id: String, val login: String, val name: String?, val avatarUrl: String?,
        val tokenType: String?, val scopes: List<String>
    )

    @Serializable
    data class Backup(
        val version: Int = 1,
        val createdAt: Long = System.currentTimeMillis(),
        val appVersion: String = BuildInfo.VERSION_NAME,
        // Appearance
        val theme: String = "system",
        val accent: String = "blue",
        val dynamicColor: Boolean = false,
        val amoled: Boolean = false,
        val fontScale: Float = 1.0f,
        val monoFontScale: Float = 1.0f,
        val density: String = "comfortable",
        val cornerRadius: Int = 14,
        val terminalTheme: String = "github-dark",
        // Behavior
        val biometricEnabled: Boolean = false,
        val autoLockMinutes: Int = 5,
        val dangerousMode: Boolean = false,
        val authorName: String? = null,
        val authorEmail: String? = null,
        // Accounts (no tokens!)
        val accounts: List<AccountStub> = emptyList()
    )

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun snapshot(am: AccountManager): Backup = Backup(
        theme = am.themeFlow.first(),
        accent = am.accentColorFlow.first(),
        dynamicColor = am.dynamicColorFlow.first(),
        amoled = am.amoledFlow.first(),
        fontScale = am.fontScaleFlow.first(),
        monoFontScale = am.monoFontScaleFlow.first(),
        density = am.densityFlow.first(),
        cornerRadius = am.cornerRadiusFlow.first(),
        terminalTheme = am.terminalThemeFlow.first(),
        biometricEnabled = am.biometricEnabledFlow.first(),
        autoLockMinutes = am.autoLockMinutesFlow.first(),
        dangerousMode = am.dangerousModeFlow.first(),
        authorName = am.authorNameFlow.first(),
        authorEmail = am.authorEmailFlow.first(),
        accounts = am.accountsBlocking().map {
            AccountStub(it.id, it.login, it.name, it.avatarUrl, it.tokenType, it.scopes)
        }
    )

    fun encode(b: Backup): String = json.encodeToString(b)
    fun decode(text: String): Backup = json.decodeFromString(text)

    fun writeToUri(ctx: Context, uri: Uri, text: String) {
        ctx.contentResolver.openOutputStream(uri, "w")?.use { it.write(text.toByteArray()) }
    }

    fun readFromUri(ctx: Context, uri: Uri): String? =
        ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }

    /** Apply the non-secret pieces of a backup. Tokens are never restored. */
    suspend fun apply(am: AccountManager, b: Backup) {
        am.setTheme(b.theme)
        am.setAccent(b.accent)
        am.setDynamicColor(b.dynamicColor)
        am.setAmoled(b.amoled)
        am.setFontScale(b.fontScale)
        am.setMonoFontScale(b.monoFontScale)
        am.setDensity(b.density)
        am.setCornerRadius(b.cornerRadius)
        am.setTerminalTheme(b.terminalTheme)
        am.setBiometric(b.biometricEnabled)
        am.setAutoLockMinutes(b.autoLockMinutes)
        am.setDangerous(b.dangerousMode)
        am.setAuthor(b.authorName, b.authorEmail)
    }
}
