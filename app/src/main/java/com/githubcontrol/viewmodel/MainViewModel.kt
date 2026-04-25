package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.GhUser
import com.githubcontrol.data.auth.Account
import com.githubcontrol.data.auth.AccountManager
import com.githubcontrol.data.auth.SessionGate
import com.githubcontrol.data.auth.TokenValidator
import com.githubcontrol.data.repository.GitHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AuthState(
    val loggedIn: Boolean = false,
    val locked: Boolean = true,
    val activeLogin: String? = null,
    val activeAvatar: String? = null,
    val rateRemaining: Int? = null,
    val accounts: List<Account> = emptyList()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    val accountManager: AccountManager,
    private val sessionGate: SessionGate,
    private val repo: GitHubRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _loginBusy = MutableStateFlow(false)
    val loginBusy: StateFlow<Boolean> = _loginBusy.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val accounts = accountManager.accountsBlocking()
            val active = accountManager.activeAccount()
            _state.value = _state.value.copy(
                loggedIn = active != null,
                accounts = accounts,
                activeLogin = active?.login,
                activeAvatar = active?.avatarUrl,
                locked = sessionGate.locked.value
            )
        }
    }

    fun signInWithToken(token: String, onDone: () -> Unit) {
        if (token.isBlank()) { _loginError.value = "Token is empty"; return }
        viewModelScope.launch {
            _loginBusy.value = true
            _loginError.value = null
            val result = withContext(Dispatchers.IO) { TokenValidator.validate(token) }
            if (!result.ok || result.login == null) {
                _loginError.value = result.error ?: "Invalid token"
                _loginBusy.value = false
                return@launch
            }
            val acc = Account(
                id = result.login!!,
                login = result.login!!,
                avatarUrl = result.avatarUrl ?: "",
                name = result.name,
                email = result.email,
                token = token,
                scopes = result.scopes
            )
            accountManager.addOrReplaceAccount(acc, makeActive = true)
            sessionGate.unlock()
            refresh()
            _loginBusy.value = false
            onDone()
        }
    }

    fun unlock() { sessionGate.unlock(); refresh() }
    fun lock() { sessionGate.lock(); refresh() }

    fun logoutActive() {
        viewModelScope.launch {
            val a = accountManager.activeAccount() ?: return@launch
            accountManager.removeAccount(a.id)
            refresh()
        }
    }

    fun switchAccount(id: String) {
        viewModelScope.launch {
            accountManager.setActive(id)
            sessionGate.unlock()
            refresh()
        }
    }

    fun wipeAll() {
        viewModelScope.launch { accountManager.wipe(); sessionGate.lock(); refresh() }
    }
}
