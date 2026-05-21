package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ModViewModel
// ... other imports

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: ModViewModel = viewModel()
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "mod_manager") {
                    composable("mod_manager") {
                        ModManagerScreen(
                            onNavigateToDownload = { navController.navigate("download") },
                            viewModel = viewModel
                        )
                    }
                    composable("download") {
                        DownloadModScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModManagerScreen(onNavigateToDownload: () -> Unit, viewModel: ModViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var modFiles by remember { mutableStateOf<List<DocumentFile>>(emptyList()) }

    var modIcons by remember { mutableStateOf<Map<Uri, Uri?>>(emptyMap()) }

    suspend fun refreshListSuspending(uri: Uri): List<DocumentFile> = withContext(Dispatchers.IO) {
        val docFile = DocumentFile.fromTreeUri(context, uri)
        val files = docFile?.listFiles()?.filter { file ->
            !file.isDirectory && (file.name?.endsWith(".jar") == true || file.name?.endsWith(".jar.disabled") == true)
        } ?: emptyList()
        
        val newIcons = mutableMapOf<Uri, Uri?>()
        for (file in files) {
           val modName = file.name?.removeSuffix(".jar")?.removeSuffix(".disabled")
           val imageFile = docFile?.findFile("$modName.png")
           newIcons[file.uri] = imageFile?.uri
        }
        modIcons = newIcons
        files
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, flags)
            viewModel.modsDirUri = it
            scope.launch {
                modFiles = refreshListSuspending(it)
            }
        }
    }
    
    // Refresh if we already have a URI
    LaunchedEffect(viewModel.modsDirUri) {
        viewModel.modsDirUri?.let { modFiles = refreshListSuspending(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Minecraft Mod Manager") }) }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Button(onClick = { launcher.launch(null) }) {
                Text("Select Minecraft Mods Folder")
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(modFiles) { modFile ->
                    ModItem(
                        modFile = modFile, 
                        iconUri = modIcons[modFile.uri],
                        onToggle = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val newName = if (modFile.name!!.endsWith(".disabled")) {
                                    modFile.name!!.removeSuffix(".disabled")
                                } else {
                                    modFile.name + ".disabled"
                                }
                                modFile.renameTo(newName)
                            }
                            viewModel.modsDirUri?.let { modFiles = refreshListSuspending(it) }
                        }
                    }, onDelete = {
                        scope.launch {
                            val deleted = withContext(Dispatchers.IO) { modFile.delete() }
                            if (deleted) {
                                viewModel.modsDirUri?.let { modFiles = refreshListSuspending(it) }
                                Toast.makeText(context, "Deleted ${modFile.name}", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to delete ${modFile.name}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateToDownload, modifier = Modifier.fillMaxWidth()) {
                Text("Download New Mods")
            }
        }
    }
}

@Composable
fun ModItem(modFile: DocumentFile, iconUri: Uri?, onToggle: () -> Unit, onDelete: () -> Unit) {
    val isEnabled = !modFile.name!!.endsWith(".disabled")
    
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (iconUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(iconUri),
                    contentDescription = "Mod Icon",
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Box(modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = modFile.name ?: "Unknown", modifier = Modifier.weight(1f))
            Switch(checked = isEnabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadModScreen(viewModel: ModViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<com.example.network.ModHit>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    var selectedMod by remember { mutableStateOf<com.example.network.ModHit?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Download Mods") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, "Back")
                    }
                }
            ) 
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search Mods") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = {
                isLoading = true
                scope.launch {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            com.example.network.RetrofitClient.apiService.searchMods(query)
                        }
                        results = response.hits
                    } catch (e: Exception) {
                        Toast.makeText(context, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isLoading = false
                    }
                }
            }) { Text("Search") }
            
            if (isLoading) CircularProgressIndicator()
            
            LazyColumn {
                items(results) { mod ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = {
                            selectedMod = mod
                            showSheet = true
                        }
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (mod.icon_url != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(mod.icon_url),
                                    contentDescription = "Mod Icon",
                                    modifier = Modifier.size(64.dp)
                                )
                            } else {
                                Box(modifier = Modifier.size(64.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = mod.title ?: "Unknown", style = MaterialTheme.typography.titleMedium)
                                Text(text = "By ${mod.author ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                                Text(text = "${mod.downloads ?: 0} downloads", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            selectedMod?.let { mod ->
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (mod.icon_url != null) {
                            Image(
                                painter = rememberAsyncImagePainter(mod.icon_url),
                                contentDescription = "Mod Icon",
                                modifier = Modifier.size(80.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = mod.title ?: "Unknown", style = MaterialTheme.typography.headlineSmall)
                            Text(text = "By ${mod.author ?: "Unknown"}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = mod.description ?: "", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                        val dirUri = viewModel.modsDirUri
                        if (dirUri != null && mod.title != null) {
                            scope.launch {
                                try {
                                    val versions = withContext(Dispatchers.IO) {
                                        com.example.network.RetrofitClient.apiService.getProjectVersions(mod.project_id)
                                    }
                                    val latestVersion = versions.firstOrNull()
                                    val primaryFile = latestVersion?.files?.find { it.primary } ?: latestVersion?.files?.firstOrNull()
                                    
                                    if (primaryFile != null) {
                                        Toast.makeText(context, "Starting download...", Toast.LENGTH_SHORT).show()
                                        val success = withContext(Dispatchers.IO) {
                                            downloadFile(context, primaryFile.url, dirUri, "${mod.title}.jar")
                                        }
                                        if (success) {
                                            Toast.makeText(context, "Download complete!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "No files found for this mod", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                                showSheet = false
                            }
                        } else {
                            Toast.makeText(context, "Please select mods folder first", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Download Latest Version")
                    }
                }
            }
        }
    }
}

suspend fun downloadFile(context: android.content.Context, url: String, dirUri: Uri, fileName: String): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val dir = DocumentFile.fromTreeUri(context, dirUri)
            val file = dir?.createFile("application/java-archive", fileName) ?: return@withContext false
            
            val connection = java.net.URL(url).openConnection()
            connection.connect()
            
            val input = connection.getInputStream()
            val output = context.contentResolver.openOutputStream(file.uri) ?: return@withContext false
            
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
            
            output.close()
            input.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
