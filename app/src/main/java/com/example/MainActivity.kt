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
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceGray
import com.example.network.ModHit
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: LauncherViewModel = viewModel()
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = "welcome") {
                    composable("welcome") {
                        WelcomeScreen(navController, viewModel)
                    }
                    composable("dashboard") {
                        LauncherDashboard(navController, viewModel)
                    }
                    composable("mod_manager") {
                        ModManagerScreen(
                            onNavigateToDownload = { navController.navigate("download") },
                            onBack = { navController.popBackStack() },
                            viewModel = viewModel
                        )
                    }
                    composable("download") {
                        DownloadModScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(navController, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(navController: NavController, viewModel: LauncherViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpace)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = NeonCyan.copy(alpha = 0.1f),
            border = BorderStroke(2.dp, NeonCyan)
        ) {
            Icon(
                Icons.Default.Build, // Changed RocketLaunch to Build
                contentDescription = null,
                modifier = Modifier.padding(24.dp).size(64.dp),
                tint = NeonCyan
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "CS LAUNCHER",
            style = MaterialTheme.typography.headlineLarge,
            color = NeonCyan,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp
        )
        
        Text(
            text = "NEXT-GEN JAVA EDITION ACCESS",
            style = MaterialTheme.typography.labelSmall,
            color = NeonPurple
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        var tempName by remember { mutableStateOf("") }
        OutlinedTextField(
            value = tempName,
            onValueChange = { tempName = it },
            label = { Text("Offline Player Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = NeonCyan,
                unfocusedIndicatorColor = Color.Gray
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                if (tempName.isNotBlank()) {
                    viewModel.accountName = tempName
                    navController.navigate("dashboard")
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("ENTER DISPATCH", fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherDashboard(navController: NavController, viewModel: LauncherViewModel) {
    val versions by viewModel.versions.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        containerColor = DeepSpace,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("CS DASHBOARD", color = NeonCyan) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DeepSpace),
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, null, tint = NeonCyan)
                    }
                }
            )
        },
        bottomBar = {
             Box(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(24.dp)
             ) {
                 Button(
                     onClick = {
                         if (viewModel.modsDirUri == null) {
                             Toast.makeText(context, "Select game directory in Settings first!", Toast.LENGTH_SHORT).show()
                         } else {
                             // Boot process logic conceptual
                             Toast.makeText(context, "Initializing JVM Bootstrap...", Toast.LENGTH_SHORT).show()
                         }
                     },
                     modifier = Modifier.fillMaxWidth().height(72.dp),
                     colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                     shape = RoundedCornerShape(16.dp)
                 ) {
                     Text("PLAY GAME", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                 }
             }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = NeonPurple) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.padding(8.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(text = viewModel.accountName, fontWeight = FontWeight.Bold)
                    Text(text = "Offline Mode", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TabRow(
                selectedTabIndex = viewModel.currentTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = NeonCyan
            ) {
                Tab(
                    selected = viewModel.currentTab == MainTab.OFFICIAL,
                    onClick = { viewModel.currentTab = MainTab.OFFICIAL },
                    text = { Text("OFFICIAL") }
                )
                Tab(
                    selected = viewModel.currentTab == MainTab.MODPACKS,
                    onClick = {
                        viewModel.currentTab = MainTab.MODPACKS
                        viewModel.fetchModpacks()
                    },
                    text = { Text("MODPACKS") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            viewModel.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            if (viewModel.currentTab == MainTab.OFFICIAL) {
                if (versions.isEmpty() && viewModel.errorMessage == null) {
                    CircularProgressIndicator(color = NeonCyan)
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(versions) { version ->
                            val isSelected = viewModel.selectedVersion == version
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.selectedVersion = version },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f),
                                border = if (isSelected) BorderStroke(1.dp, NeonCyan) else null
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Layers, null, tint = if (isSelected) NeonCyan else Color.Gray)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(text = version.id, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    Spacer(modifier = Modifier.weight(1f))
                                    if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = NeonCyan)
                                }
                            }
                        }
                    }
                }
            } else {
                if (viewModel.modpacks.isEmpty() && viewModel.errorMessage == null) {
                    CircularProgressIndicator(color = NeonCyan)
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(viewModel.modpacks) { modpack ->
                            val isSelected = viewModel.selectedModpack == modpack
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.selectedModpack = modpack },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) NeonPurple.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.03f),
                                border = if (isSelected) BorderStroke(1.dp, NeonPurple) else null
                            ) {
                                Text(text = modpack.title ?: "Unknown Modpack", modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { navController.navigate("mod_manager") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                border = BorderStroke(1.dp, NeonCyan)
            ) {
                Icon(Icons.Default.Folder, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("MANAGE MODS")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, flags)
            viewModel.modsDirUri = it
        }
    }

    Scaffold(
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
                title = { Text("SYSTEM SETTINGS") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSpace)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("GAME DIRECTORY", color = NeonCyan)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { launcher.launch(null) },
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = viewModel.modsDirUri?.path ?: "Not Selected", style = MaterialTheme.typography.bodySmall)
                    Text(text = "Tap to change", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            var ram by remember { mutableStateOf("2G") }
            Text("ALLOCATED RAM", color = NeonCyan)
            OutlinedTextField(
                value = ram,
                onValueChange = { ram = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("e.g. 2G, 4096M") }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("SAVE CONFIG", color = Color.Black)
            }
        }
    }
}

// Function to assist border creation
@Composable
fun border(width: androidx.compose.ui.unit.Dp, color: Color, shape: androidx.compose.ui.graphics.Shape) = 
    Modifier.border(width, color, shape)

// Re-implementing the Mod Manager screens concisely or moving them to separate files is better, 
// but for the sake of this architectural prompt, I'll provide the Launcher core first.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModManagerScreen(onNavigateToDownload: () -> Unit, onBack: () -> Unit, viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel.modsDirUri) {
        viewModel.refreshModList(context)
    }

    Scaffold(
        containerColor = DeepSpace,
        topBar = {
            TopAppBar(
                title = { Text("MOD MANAGER") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSpace)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            if (viewModel.modsDirUri == null) {
                Text("Please select your .minecraft folder in Settings first.", color = Color.LightGray)
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(viewModel.modFiles) { modFile ->
                        ModItem(
                            modFile = modFile, 
                            iconUri = viewModel.modIcons[modFile.uri],
                            onToggle = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val name = modFile.name
                                        val newName = if (name != null && name.endsWith(".disabled")) {
                                            name.removeSuffix(".disabled")
                                        } else if (name != null) {
                                            "$name.disabled"
                                        } else {
                                            null
                                        }
                                        if (newName != null) {
                                            modFile.renameTo(newName)
                                        }
                                    }
                                    viewModel.refreshModList(context)
                                }
                            }, onDelete = {
                                scope.launch {
                                    val deleted = withContext(Dispatchers.IO) { modFile.delete() }
                                    if (deleted) {
                                        viewModel.refreshModList(context)
                                        Toast.makeText(context, "Deleted ${modFile.name}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to delete ${modFile.name}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateToDownload, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)) {
                Text("GET MORE MODS")
            }
        }
    }
}

@Composable
fun ModItem(modFile: DocumentFile, iconUri: Uri?, onToggle: () -> Unit, onDelete: () -> Unit) {
    val name = modFile.name ?: "Unknown"
    val isEnabled = !name.endsWith(".disabled")
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (iconUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(iconUri),
                    contentDescription = "Mod Icon",
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(modifier = Modifier.size(48.dp).background(Color.DarkGray, RoundedCornerShape(8.dp)))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = modFile.name ?: "Unknown", modifier = Modifier.weight(1f), color = Color.White)
            Switch(checked = isEnabled, onCheckedChange = { onToggle() }, colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadModScreen(viewModel: LauncherViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ModHit>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    var selectedMod by remember { mutableStateOf<ModHit?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    
    var searchJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    Scaffold(
        containerColor = DeepSpace,
        topBar = { 
            TopAppBar(
                title = { Text("MODRINTH HUB") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DeepSpace)
            ) 
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search Modrinth") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        if (query.isBlank()) return@IconButton
                        searchJob?.cancel()
                        isLoading = true
                        searchJob = scope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    RetrofitClient.apiService.searchMods(query)
                                }
                                results = response.hits
                            } catch (e: Exception) {
                                if (e !is kotlinx.coroutines.CancellationException) {
                                    Toast.makeText(context, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    }) {
                        Icon(Icons.Default.Search, null, tint = NeonCyan)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = NeonCyan)
            
            LazyColumn {
                items(results) { mod ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = {
                            selectedMod = mod
                            showSheet = true
                        },
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (mod.icon_url != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(mod.icon_url),
                                    contentDescription = "Mod Icon",
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(text = mod.title ?: "Unknown", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text(text = "by ${mod.author}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
            sheetState = sheetState,
            containerColor = SurfaceGray
        ) {
            selectedMod?.let { mod ->
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                    Text(text = mod.title ?: "Unknown", style = MaterialTheme.typography.headlineSmall, color = NeonCyan)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = mod.description ?: "", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val dirUri = viewModel.modsDirUri
                            if (dirUri != null && mod.title != null) {
                                scope.launch {
                                    try {
                                        val versions = withContext(Dispatchers.IO) {
                                            RetrofitClient.apiService.getProjectVersions(mod.project_id)
                                        }
                                        val latestVersion = versions.firstOrNull()
                                        val primaryFile = latestVersion?.files?.find { it.primary } ?: latestVersion?.files?.firstOrNull()
                                        
                                        if (primaryFile != null) {
                                            Toast.makeText(context, "Transmitting data...", Toast.LENGTH_SHORT).show()
                                            val success = withContext(Dispatchers.IO) {
                                                downloadFile(context, primaryFile.url, dirUri, "${mod.title}.jar")
                                            }
                                            if (success) {
                                                Toast.makeText(context, "Mod Installed Successfully!", Toast.LENGTH_SHORT).show()
                                                viewModel.refreshModList(context)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Transmission Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                    showSheet = false
                                }
                            } else {
                                Toast.makeText(context, "System Error: Missing Root Directory", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                    ) {
                        Text("INITIATE DOWNLOAD", fontWeight = FontWeight.Bold)
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
            
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val inputStream = connection.inputStream
            val outputStream = context.contentResolver.openOutputStream(file.uri) ?: return@withContext false
            
            inputStream.use { input ->
                outputStream.use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("DownloadFile", "Error downloading mod", e)
            false
        }
    }
}
