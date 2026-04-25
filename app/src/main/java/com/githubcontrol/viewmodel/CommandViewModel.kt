package com.githubcontrol.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.CreateIssueRequest
import com.githubcontrol.data.api.CreateRepoRequest
import com.githubcontrol.data.auth.AccountManager
import com.githubcontrol.data.db.AppDatabase
import com.githubcontrol.data.db.CommandHistoryEntity
import com.githubcontrol.data.repository.GitHubRepository
import com.githubcontrol.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CommandLine(val cmd: String, val output: String, val ok: Boolean)

data class CommandState(
    val input: String = "",
    val lines: List<CommandLine> = emptyList(),
    val running: Boolean = false
)

@HiltViewModel
class CommandViewModel @Inject constructor(
    private val repo: GitHubRepository,
    private val db: AppDatabase,
    private val accounts: AccountManager
) : ViewModel() {
    private val _state = MutableStateFlow(CommandState())
    val state: StateFlow<CommandState> = _state

    fun setInput(t: String) { _state.value = _state.value.copy(input = t) }

    fun run() {
        val raw = _state.value.input.trim()
        if (raw.isEmpty()) return
        _state.value = _state.value.copy(running = true)
        viewModelScope.launch {
            Logger.i("Command", "$ $raw")
            val out = execute(raw)
            if (out.ok) Logger.i("Command", out.output.lineSequence().firstOrNull().orEmpty())
            else Logger.e("Command", out.output)
            val accId = accounts.activeAccount()?.id ?: "anon"
            db.commandHistory().insert(CommandHistoryEntity(accountId = accId, command = raw, output = out.output, success = out.ok))
            _state.value = CommandState(input = "", lines = _state.value.lines + CommandLine(raw, out.output, out.ok), running = false)
        }
    }

    private suspend fun execute(line: String): CommandLine {
        val parts = tokenize(line)
        val cmd = parts.firstOrNull() ?: return CommandLine(line, "Empty", false)
        val args = parts.drop(1)
        return try {
            when (cmd) {
                "help" -> CommandLine(line, """Commands:
help
me
rate
repo create <name> [--private] [--desc=...]
repo delete <owner>/<name>
repo rename <owner>/<name> <newName>
repo star <owner>/<name>
repo unstar <owner>/<name>
repo fork <owner>/<name>
branch list <owner>/<name>
branch create <owner>/<name> <new> from <base>
branch delete <owner>/<name> <branch>
issue create <owner>/<name> "title" "body"
issue close <owner>/<name> <number>
search repos <query>
search code <query>
notifications
""", true)
                "me" -> {
                    val u = repo.me()
                    CommandLine(line, "${u.login} (${u.name ?: ""}) — ${u.publicRepos} public repos", true)
                }
                "rate" -> {
                    val r = repo.rateLimit().rate
                    CommandLine(line, "core: ${r.remaining}/${r.limit} (resets ${r.reset})", true)
                }
                "repo" -> repoCmd(args, line)
                "branch" -> branchCmd(args, line)
                "issue" -> issueCmd(args, line)
                "search" -> searchCmd(args, line)
                "notifications" -> {
                    val n = repo.notifications()
                    CommandLine(line, n.joinToString("\n") { "${it.repository.fullName}: ${it.subject.title}" }, true)
                }
                else -> CommandLine(line, "Unknown command. Try 'help'.", false)
            }
        } catch (t: Throwable) { CommandLine(line, "ERR: ${t.message}", false) }
    }

    private suspend fun repoCmd(args: List<String>, line: String): CommandLine {
        if (args.isEmpty()) return CommandLine(line, "Usage: repo <create|delete|rename|star|unstar|fork> ...", false)
        return when (args[0]) {
            "create" -> {
                val name = args.getOrNull(1) ?: return CommandLine(line, "Name required", false)
                val priv = args.contains("--private")
                val desc = args.firstOrNull { it.startsWith("--desc=") }?.substringAfter("--desc=")
                val r = repo.createRepo(CreateRepoRequest(name = name, private = priv, description = desc))
                CommandLine(line, "Created ${r.fullName}", true)
            }
            "delete" -> { val (o, n) = ownerName(args[1]); repo.deleteRepo(o, n); CommandLine(line, "Deleted $o/$n", true) }
            "rename" -> {
                val (o, n) = ownerName(args[1])
                val newName = args.getOrNull(2) ?: return CommandLine(line, "Need new name", false)
                val r = repo.updateRepo(o, n, com.githubcontrol.data.api.UpdateRepoRequest(name = newName))
                CommandLine(line, "Renamed → ${r.fullName}", true)
            }
            "star" -> { val (o, n) = ownerName(args[1]); repo.star(o, n); CommandLine(line, "Starred", true) }
            "unstar" -> { val (o, n) = ownerName(args[1]); repo.unstar(o, n); CommandLine(line, "Unstarred", true) }
            "fork" -> { val (o, n) = ownerName(args[1]); val r = repo.forkRepo(o, n); CommandLine(line, "Forked → ${r.fullName}", true) }
            else -> CommandLine(line, "Unknown repo subcmd", false)
        }
    }

    private suspend fun branchCmd(args: List<String>, line: String): CommandLine {
        if (args.size < 2) return CommandLine(line, "Usage: branch <list|create|delete> <owner>/<name> ...", false)
        val (o, n) = ownerName(args[1])
        return when (args[0]) {
            "list" -> CommandLine(line, repo.branches(o, n).joinToString(", ") { it.name }, true)
            "create" -> {
                val newName = args.getOrNull(2) ?: return CommandLine(line, "Need new branch name", false)
                val from = args.getOrNull(4) ?: "main"
                repo.createBranch(o, n, newName, from); CommandLine(line, "Created $newName from $from", true)
            }
            "delete" -> {
                val br = args.getOrNull(2) ?: return CommandLine(line, "Need branch", false)
                repo.deleteBranch(o, n, br); CommandLine(line, "Deleted $br", true)
            }
            else -> CommandLine(line, "Unknown branch subcmd", false)
        }
    }

    private suspend fun issueCmd(args: List<String>, line: String): CommandLine {
        if (args.size < 2) return CommandLine(line, "Usage: issue <create|close> <owner>/<name> ...", false)
        val (o, n) = ownerName(args[1])
        return when (args[0]) {
            "create" -> {
                val title = args.getOrNull(2) ?: return CommandLine(line, "title required", false)
                val body = args.getOrNull(3)
                val i = repo.createIssue(o, n, CreateIssueRequest(title = title, body = body))
                CommandLine(line, "#${i.number} ${i.title}", true)
            }
            "close" -> {
                val number = args.getOrNull(2)?.toIntOrNull() ?: return CommandLine(line, "issue # required", false)
                repo.updateIssue(o, n, number, com.githubcontrol.data.api.UpdateIssueRequest(state = "closed"))
                CommandLine(line, "Closed #$number", true)
            }
            else -> CommandLine(line, "Unknown issue subcmd", false)
        }
    }

    private suspend fun searchCmd(args: List<String>, line: String): CommandLine {
        if (args.size < 2) return CommandLine(line, "Usage: search <repos|code|users> <query>", false)
        val q = args.drop(1).joinToString(" ")
        return when (args[0]) {
            "repos" -> CommandLine(line, repo.searchRepos(q).items.take(10).joinToString("\n") { "${it.fullName} ★${it.stars}" }, true)
            "code" -> CommandLine(line, repo.searchCode(q).items.take(10).joinToString("\n") { "${it.repository.fullName}: ${it.path}" }, true)
            "users" -> CommandLine(line, repo.searchUsers(q).items.take(10).joinToString("\n") { it.login }, true)
            else -> CommandLine(line, "Unknown search type", false)
        }
    }

    private fun ownerName(s: String): Pair<String, String> {
        val parts = s.split("/")
        return parts[0] to parts.getOrElse(1) { "" }
    }

    private fun tokenize(s: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQ = false
        for (c in s) {
            when {
                c == '"' -> inQ = !inQ
                c.isWhitespace() && !inQ -> { if (cur.isNotEmpty()) { out += cur.toString(); cur.clear() } }
                else -> cur.append(c)
            }
        }
        if (cur.isNotEmpty()) out += cur.toString()
        return out
    }
}
