package com.githubcontrol.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionGate @Inject constructor() {
    private val _locked = MutableStateFlow(true)
    val locked: StateFlow<Boolean> = _locked

    fun lock() { _locked.value = true }
    fun unlock() { _locked.value = false }
}
