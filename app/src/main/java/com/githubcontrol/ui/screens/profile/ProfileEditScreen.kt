package com.githubcontrol.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.githubcontrol.data.api.UpdateUserRequest
import com.githubcontrol.data.repository.GitHubRepository
import com.githubcontrol.ui.components.EmbeddedTerminal
import com.githubcontrol.ui.components.GhCard
import com.githubcontrol.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileForm(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val name: String = "", val email: String = "", val blog: String = "",
    val bio: String = "", val company: String = "", val location: String = "",
    val twitter: String = "", val hireable: Boolean = false,
    val message: String? = null, val error: String? = null
)

@HiltViewModel
class ProfileEditViewModel @Inject constructor(private val repo: GitHubRepository) : ViewModel() {
    val form = MutableStateFlow(ProfileForm())
    init {
        viewModelScope.launch {
            try {
                val u = repo.api.me()
                form.value = ProfileForm(
                    loading = false, name = u.name.orEmpty(), email = u.email.orEmpty(),
                    blog = u.blog.orEmpty(), bio = u.bio.orEmpty(),
                    company = u.company.orEmpty(), location = u.location.orEmpty(),
                    twitter = u.twitterUsername.orEmpty(), hireable = u.hireable ?: false
                )
            } catch (t: Throwable) {
                form.value = ProfileForm(loading = false, error = t.message)
            }
        }
    }
    fun update(transform: (ProfileForm) -> ProfileForm) { form.value = transform(form.value) }
    fun save() {
        val f = form.value
        viewModelScope.launch {
            form.value = f.copy(saving = true, error = null, message = null)
            try {
                repo.updateMe(UpdateUserRequest(
                    name = f.name.ifBlank { null }, email = f.email.ifBlank { null },
                    blog = f.blog.ifBlank { null }, bio = f.bio.ifBlank { null },
                    company = f.company.ifBlank { null }, location = f.location.ifBlank { null },
                    twitterUsername = f.twitter.ifBlank { null }, hireable = f.hireable
                ))
                Logger.i("Profile", "saved profile updates")
                form.value = f.copy(saving = false, message = "Saved.")
            } catch (t: Throwable) {
                Logger.e("Profile", "save failed", t)
                form.value = f.copy(saving = false, error = t.message)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(onBack: () -> Unit, vm: ProfileEditViewModel = hiltViewModel()) {
    val f by vm.form.collectAsState()
    Scaffold(topBar = {
        TopAppBar(title = { Text("Edit profile") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } })
    }) { pad ->
        Column(Modifier.padding(pad).padding(12.dp).fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (f.loading) { LinearProgressIndicator(Modifier.fillMaxWidth()); return@Column }
            f.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            f.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            GhCard {
                Text("Public profile", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(f.name, { v -> vm.update { it.copy(name = v) } }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(f.email, { v -> vm.update { it.copy(email = v) } }, label = { Text("Public email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(f.blog, { v -> vm.update { it.copy(blog = v) } }, label = { Text("Website") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(f.company, { v -> vm.update { it.copy(company = v) } }, label = { Text("Company") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(f.location, { v -> vm.update { it.copy(location = v) } }, label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(f.twitter, { v -> vm.update { it.copy(twitter = v) } }, label = { Text("Twitter / X username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(f.bio, { v -> vm.update { it.copy(bio = v) } }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp))
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Switch(f.hireable, { v -> vm.update { it.copy(hireable = v) } })
                    Spacer(Modifier.width(8.dp)); Text("Available for hire")
                }
            }
            Button(onClick = { vm.save() }, enabled = !f.saving, modifier = Modifier.fillMaxWidth()) {
                if (f.saving) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                else { Icon(Icons.Filled.Save, null); Spacer(Modifier.width(6.dp)); Text("Save changes") }
            }
            EmbeddedTerminal(section = "Profile")
        }
    }
}
