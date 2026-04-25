package com.githubcontrol.data.auth

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val login: String,
    val avatarUrl: String,
    val name: String? = null,
    val email: String? = null,
    val token: String,
    val scopes: List<String> = emptyList(),
    val addedAt: Long = System.currentTimeMillis()
)
