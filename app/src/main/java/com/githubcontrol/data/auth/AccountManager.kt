package com.githubcontrol.data.auth

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("settings")

@Singleton
class AccountManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val secure = EncryptedSharedPreferences.create(
        context, "secure_accounts", masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val keyAccounts = "accounts_json"
    private val keyActive = stringPreferencesKey("active_account_id")
    private val keyBiometric = booleanPreferencesKey("biometric_enabled")
    private val keyAutoLockMinutes = intPreferencesKey("auto_lock_minutes")
    private val keyTheme = stringPreferencesKey("theme_mode") // light/dark/system
    private val keyDangerous = booleanPreferencesKey("dangerous_mode")
    private val keyLastActive = longPreferencesKey("last_active_at")
    private val keyAuthorName = stringPreferencesKey("author_name")
    private val keyAuthorEmail = stringPreferencesKey("author_email")
    private val keyDefaultBranch = stringPreferencesKey("default_branch_pref")

    val rateRemaining = MutableStateFlow<Int?>(null)
    fun updateRateRemaining(v: Int) { rateRemaining.value = v }

    val accountsFlow: Flow<List<Account>> = context.dataStore.data.map {
        // accounts are in encrypted prefs, but we still need a stream trigger
        loadAccounts()
    }

    val activeAccountIdFlow: Flow<String?> = context.dataStore.data.map { it[keyActive] }
    val biometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[keyBiometric] ?: false }
    val autoLockMinutesFlow: Flow<Int> = context.dataStore.data.map { it[keyAutoLockMinutes] ?: 5 }
    val themeFlow: Flow<String> = context.dataStore.data.map { it[keyTheme] ?: "system" }
    val dangerousModeFlow: Flow<Boolean> = context.dataStore.data.map { it[keyDangerous] ?: false }
    val authorNameFlow: Flow<String?> = context.dataStore.data.map { it[keyAuthorName] }
    val authorEmailFlow: Flow<String?> = context.dataStore.data.map { it[keyAuthorEmail] }

    private fun loadAccounts(): List<Account> {
        val raw = secure.getString(keyAccounts, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Account>>(raw) }.getOrDefault(emptyList())
    }

    private fun saveAccounts(list: List<Account>) {
        secure.edit().putString(keyAccounts, json.encodeToString(list)).apply()
    }

    suspend fun activeAccount(): Account? {
        val id = activeAccountIdFlow.first() ?: return loadAccounts().firstOrNull()
        return loadAccounts().firstOrNull { it.id == id } ?: loadAccounts().firstOrNull()
    }

    suspend fun activeToken(): String? = activeAccount()?.token

    suspend fun addOrReplaceAccount(account: Account, makeActive: Boolean = true) {
        val list = loadAccounts().toMutableList()
        val idx = list.indexOfFirst { it.id == account.id }
        if (idx >= 0) list[idx] = account else list.add(account)
        saveAccounts(list)
        if (makeActive) setActive(account.id)
    }

    suspend fun setActive(id: String) {
        context.dataStore.edit { it[keyActive] = id }
    }

    suspend fun removeAccount(id: String) {
        val list = loadAccounts().filterNot { it.id == id }
        saveAccounts(list)
        if (activeAccountIdFlow.first() == id) {
            val next = list.firstOrNull()?.id
            context.dataStore.edit {
                if (next == null) it.remove(keyActive) else it[keyActive] = next
            }
        }
    }

    suspend fun wipe() {
        secure.edit().clear().apply()
        context.dataStore.edit { it.clear() }
    }

    fun accountsBlocking(): List<Account> = loadAccounts()

    suspend fun setBiometric(enabled: Boolean) = context.dataStore.edit { it[keyBiometric] = enabled }
    suspend fun setAutoLockMinutes(min: Int) = context.dataStore.edit { it[keyAutoLockMinutes] = min }
    suspend fun setTheme(mode: String) = context.dataStore.edit { it[keyTheme] = mode }
    suspend fun setDangerous(enabled: Boolean) = context.dataStore.edit { it[keyDangerous] = enabled }
    suspend fun touchActivity() = context.dataStore.edit { it[keyLastActive] = System.currentTimeMillis() }
    suspend fun lastActiveAt(): Long = context.dataStore.data.first()[keyLastActive] ?: 0L
    suspend fun setAuthor(name: String?, email: String?) = context.dataStore.edit {
        if (name == null) it.remove(keyAuthorName) else it[keyAuthorName] = name
        if (email == null) it.remove(keyAuthorEmail) else it[keyAuthorEmail] = email
    }

    /** Update the cached OAuth scopes for an existing account (parsed from response headers). */
    suspend fun updateScopes(id: String, scopes: List<String>) {
        val list = loadAccounts().toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx >= 0) {
            list[idx] = list[idx].copy(scopes = scopes)
            saveAccounts(list)
        }
    }

    /** Append a new validation record to the account's history (kept bounded). */
    suspend fun recordValidation(id: String, validation: TokenValidation, refreshScopes: Boolean = true) {
        val list = loadAccounts().toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx < 0) return
        val current = list[idx]
        val newHistory = (listOf(validation) + current.validations).take(20)
        list[idx] = current.copy(
            validations = newHistory,
            lastValidatedAt = validation.ts,
            scopes = if (refreshScopes && validation.ok) validation.scopes.ifEmpty { current.scopes } else current.scopes,
            tokenType = validation.tokenType ?: current.tokenType,
            tokenExpiry = validation.tokenExpiry ?: current.tokenExpiry
        )
        saveAccounts(list)
    }

    /** Returns the list of recommended scopes that are missing from the active token. */
    suspend fun missingScopes(required: List<String> = ScopeCatalog.recommended): List<String> {
        val have = activeAccount()?.scopes ?: return required
        return required.filterNot { req -> have.any { it == req || it.startsWith("$req:") } }
    }
}
