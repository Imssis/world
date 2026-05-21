package com.example

import androidx.lifecycle.ViewModel
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import com.example.network.MojangVersion
import com.example.network.ModHit
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainTab { OFFICIAL, MODPACKS }

class LauncherViewModel : ViewModel() {
    var modsDirUri by mutableStateOf<Uri?>(null)
    var modFiles by mutableStateOf<List<DocumentFile>>(emptyList())
    var modIcons by mutableStateOf<Map<Uri, Uri?>>(emptyMap())
    
    var currentTab by mutableStateOf(MainTab.OFFICIAL)
    
    private val _versions = MutableStateFlow<List<MojangVersion>>(emptyList())
    val versions: StateFlow<List<MojangVersion>> = _versions
    
    var modpacks by mutableStateOf<List<ModHit>>(emptyList())
    var selectedModpackDetails by mutableStateOf<List<com.example.network.ModVersion>?>(null)
    var isInstalling by mutableStateOf(false)
    
    var selectedVersion by mutableStateOf<MojangVersion?>(null)
    var selectedModpack by mutableStateOf<ModHit?>(null)

    fun onModpackSelected(modpack: ModHit?) {
        selectedModpack = modpack
        if (modpack != null) {
            inspectModpack(modpack.project_id)
        }
    }
    
    var accountName by mutableStateOf("Player")
    var errorMessage by mutableStateOf<String?>(null)

    init {
        fetchVersions()
    }

    private fun inspectModpack(projectId: String) {
        viewModelScope.launch {
            try {
                selectedModpackDetails = RetrofitClient.apiService.getProjectVersions(projectId)
            } catch (e: Exception) {
                errorMessage = "Failed to inspect modpack: ${e.message}"
            }
        }
    }
    
    fun installModpackPipeline(projectId: String, context: android.content.Context) {
        viewModelScope.launch {
            isInstalling = true
            try {
                val versions = RetrofitClient.apiService.getProjectVersions(projectId)
                val latest = versions.firstOrNull()
                // Assuming we want to download all files in the latest version for simplicity
                val files = latest?.files ?: emptyList()
                
                // Pipeline logic would go here
                
                Log.d("LauncherViewModel", "Installing ${files.size} files for $projectId")
            } catch (e: Exception) {
                errorMessage = "Installation failed: ${e.message}"
            } finally {
                isInstalling = false
            }
        }
    }

    fun refreshModList(context: android.content.Context) {
        val uri = modsDirUri ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                val docFile = DocumentFile.fromTreeUri(context, uri)
                val files = docFile?.listFiles()?.filter { file ->
                    !file.isDirectory && (file.name?.endsWith(".jar") == true || file.name?.endsWith(".jar.disabled") == true)
                } ?: emptyList()
                
                val icons = mutableMapOf<Uri, Uri?>()
                for (file in files) {
                    val modName = file.name?.removeSuffix(".jar")?.removeSuffix(".disabled")
                    val imageFile = docFile?.findFile("$modName.png")
                    icons[file.uri] = imageFile?.uri
                }
                files to icons
            }
            modFiles = result.first
            modIcons = result.second
        }
    }

    fun fetchVersions() {
        viewModelScope.launch {
            try {
                Log.d("LauncherViewModel", "Fetching versions...")
                val manifest = RetrofitClient.mojangApiService.getVersionManifest()
                val releases = manifest.versions.filter { it.type == "release" }
                _versions.value = releases
                if (releases.isNotEmpty() && selectedVersion == null) {
                    selectedVersion = releases.first()
                }
                Log.d("LauncherViewModel", "Fetched ${releases.size} versions")
            } catch (e: Exception) {
                Log.e("LauncherViewModel", "Error fetching versions", e)
                errorMessage = "Network Error: ${e.message}. Showing cached versions."
                // In a real app, you would load from cache here
            }
        }
    }

    fun fetchModpacks() {
        if (modpacks.isNotEmpty()) return
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.searchModpacks()
                modpacks = response.hits
            } catch (e: Exception) {
                Log.e("LauncherViewModel", "Error fetching modpacks", e)
                errorMessage = "Error fetching modpacks: ${e.message}"
            }
        }
    }
}
