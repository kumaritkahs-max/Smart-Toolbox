package com.githubcontrol.data.auth

import kotlinx.serialization.Serializable

/** A single past validation of this account's PAT. */
@Serializable
data class TokenValidation(
    val ts: Long,
    val ok: Boolean,
    val httpCode: Int? = null,
    val scopes: List<String> = emptyList(),
    val acceptedScopes: List<String> = emptyList(),
    val rateLimit: Int? = null,
    val rateMax: Int? = null,
    val tokenType: String? = null,
    val tokenExpiry: String? = null,
    val error: String? = null
)

@Serializable
data class Account(
    val id: String,
    val login: String,
    val avatarUrl: String,
    val name: String? = null,
    val email: String? = null,
    val token: String,
    val scopes: List<String> = emptyList(),
    val addedAt: Long = System.currentTimeMillis(),
    val tokenType: String? = null,         // "classic" | "fine-grained" | unknown
    val tokenExpiry: String? = null,       // header GitHub-Authentication-Token-Expiration
    val lastValidatedAt: Long = 0L,
    val validations: List<TokenValidation> = emptyList()
)

/**
 * Static catalogue of well-known OAuth/PAT scopes with a friendly description
 * and a "danger" hint used to colour-code permission lists in the UI.
 */
object ScopeCatalog {
    enum class Risk { LOW, MEDIUM, HIGH, CRITICAL }
    data class Info(val scope: String, val title: String, val description: String, val risk: Risk)

    private val list: List<Info> = listOf(
        Info("repo", "Repositories — full control",
            "Read & write access to public and private repositories, including code, commits, branches, pull requests, issues, and webhooks.", Risk.HIGH),
        Info("repo:status", "Commit statuses",
            "Read & write commit statuses (CI green/red checkmarks).", Risk.LOW),
        Info("repo_deployment", "Deployments",
            "Create, view and update deployments and deployment statuses.", Risk.MEDIUM),
        Info("public_repo", "Public repositories",
            "Same as 'repo' but limited to public repositories only.", Risk.MEDIUM),
        Info("repo:invite", "Repository invitations",
            "Accept and decline collaborator invitations.", Risk.LOW),
        Info("security_events", "Code security events",
            "Read & write code-scanning and secret-scanning alerts.", Risk.MEDIUM),
        Info("workflow", "GitHub Actions workflows",
            "Update workflow files in .github/workflows. Required to push YAML changes that touch CI.", Risk.HIGH),
        Info("write:packages", "Packages — write",
            "Upload and publish packages to GitHub Packages.", Risk.MEDIUM),
        Info("read:packages", "Packages — read",
            "Download packages from GitHub Packages.", Risk.LOW),
        Info("delete:packages", "Packages — delete",
            "Delete packages from GitHub Packages.", Risk.HIGH),
        Info("admin:org", "Organisation admin",
            "Full administrative access to organisations, including teams and membership.", Risk.CRITICAL),
        Info("write:org", "Organisation — write",
            "Read & write organisation membership and team details.", Risk.HIGH),
        Info("read:org", "Organisation — read",
            "Read organisation membership and team details.", Risk.LOW),
        Info("admin:public_key", "SSH keys — admin",
            "Full control over user-level SSH public keys.", Risk.HIGH),
        Info("write:public_key", "SSH keys — write",
            "Create user-level SSH public keys.", Risk.MEDIUM),
        Info("read:public_key", "SSH keys — read",
            "Read user-level SSH public keys.", Risk.LOW),
        Info("admin:repo_hook", "Repository webhooks — admin",
            "Read, write, ping and delete repository webhooks.", Risk.HIGH),
        Info("write:repo_hook", "Repository webhooks — write",
            "Create and edit repository webhooks.", Risk.MEDIUM),
        Info("read:repo_hook", "Repository webhooks — read",
            "List repository webhook configuration.", Risk.LOW),
        Info("admin:org_hook", "Organisation webhooks",
            "Manage organisation-wide webhooks.", Risk.HIGH),
        Info("gist", "Gists",
            "Create, edit and delete the user's gists.", Risk.MEDIUM),
        Info("notifications", "Notifications",
            "Read and mark notifications as done.", Risk.LOW),
        Info("user", "User profile — full",
            "Read & write user profile details, follow others, view email addresses.", Risk.MEDIUM),
        Info("read:user", "User profile — read",
            "Read your profile data.", Risk.LOW),
        Info("user:email", "Email addresses",
            "Read user email addresses.", Risk.MEDIUM),
        Info("user:follow", "Follow users",
            "Follow and unfollow other users.", Risk.LOW),
        Info("delete_repo", "Delete repositories",
            "Permanently delete repositories — destructive.", Risk.CRITICAL),
        Info("write:discussion", "Discussions — write",
            "Create and edit team discussions.", Risk.MEDIUM),
        Info("read:discussion", "Discussions — read",
            "Read team discussions.", Risk.LOW),
        Info("codespace", "Codespaces",
            "Create and manage codespaces.", Risk.MEDIUM),
        Info("copilot", "Copilot",
            "Manage Copilot Business seats and configuration.", Risk.MEDIUM),
        Info("admin:gpg_key", "GPG keys — admin",
            "Full control over GPG keys.", Risk.HIGH),
        Info("write:gpg_key", "GPG keys — write",
            "Add new GPG keys.", Risk.MEDIUM),
        Info("read:gpg_key", "GPG keys — read",
            "Read GPG keys.", Risk.LOW),
        Info("admin:ssh_signing_key", "SSH signing keys — admin",
            "Full control over SSH signing keys.", Risk.HIGH),
        Info("admin:enterprise", "Enterprise admin",
            "Full administrative access to enterprise resources.", Risk.CRITICAL),
        Info("manage_billing:enterprise", "Enterprise billing",
            "Read and write enterprise billing data.", Risk.HIGH),
        Info("manage_runners:enterprise", "Enterprise runners",
            "Manage self-hosted runners at the enterprise level.", Risk.HIGH),
        Info("project", "Projects",
            "Read & write classic projects.", Risk.MEDIUM),
        Info("read:project", "Projects — read",
            "Read classic projects.", Risk.LOW),
    )

    private val byScope = list.associateBy { it.scope }

    fun describe(scope: String): Info =
        byScope[scope] ?: Info(scope, scope, "Custom or unrecognised scope.", Risk.MEDIUM)

    /** Recommended scopes for this app's full feature set. */
    val recommended: List<String> = listOf(
        "repo", "workflow", "user", "read:org", "notifications",
        "write:public_key", "write:discussion", "delete_repo", "gist"
    )
}
