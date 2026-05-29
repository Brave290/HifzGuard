package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.service.OverlayService
import com.example.ui.HifzViewModel
import com.example.ui.HifzViewModelFactory
import com.example.ui.screens.*
import com.example.ui.theme.DarkCard
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.HifzGuardTheme
import com.example.integrity.SignatureVerifier
import com.example.integrity.RootDetector

enum class HifzScreen {
    Dashboard,
    ReadQuran,
    Session,
    JuzGrid,
    Statistics,
    Settings
}

class MainActivity : ComponentActivity() {

    companion object {
        var isAppInForeground = false
    }

    private fun safeStartOverlayService(actionString: String) {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = actionString
        }
        try {
            // Try starting service normally first (safe if service already foregrounded or if app is in foreground)
            startService(intent)
        } catch (e: Exception) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            } catch (ex: Exception) {
                Log.e("MainActivity", "Failed to start overlay service safely with action: $actionString", ex)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isAppInForeground = true
        safeStartOverlayService(OverlayService.ACTION_APP_FOREGROUND)
    }

    override fun onStop() {
        super.onStop()
        isAppInForeground = false
        safeStartOverlayService(OverlayService.ACTION_APP_BACKGROUND)
    }

    private lateinit var viewModel: HifzViewModel

    // Audio recording request launcher
    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startSession()
        } else {
            Toast.makeText(this, "Audio permission denied. Session will track purely via gestures.", Toast.LENGTH_LONG).show()
            viewModel.startSession()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Anti-Tampering Layer E: Screenshot & screen record prevention (Disabled in emulator-preview to prevent canvas blacking out)
        /*
        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } catch (e: Exception) {
            // Log and bypass warning log safely
        }
        */

        enableEdgeToEdge()

        // Extract application dependency container
        val app = application as HifzGuardApplication
        viewModel = HifzViewModelFactory(
            this,
            app.preferencesHelper,
            app.sessionDataStore,
            app.juzRepository
        ).create(HifzViewModel::class.java)

        // Make sure overlay service is turned on to background-monitor locking schedule
        startBaseOverlayService()

        setContent {
            HifzGuardTheme {
                val isTampered by viewModel.isTampered.collectAsState()
                val isRooted by viewModel.isRooted.collectAsState()

                if (isTampered) {
                    // Non-dismissable warning screen if modified APK detected
                    TamperedBlockerUI()
                } else {
                    MainNavigationScaffold(
                        viewModel = viewModel,
                        isRooted = isRooted,
                        onRequestAudioPermission = {
                            checkAndRequestAudioPermission()
                        },
                        onRequestOverlayPermission = {
                            promptOverlaySystemPermission()
                        }
                    )
                }
            }
        }
    }

    private fun startBaseOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            // Bypass gracefully if foreground service throws during initial build configurations
        }
    }

    private fun checkAndRequestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startSession()
        } else {
            requestAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun promptOverlaySystemPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Please search overlay permissions manually in settings.", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun MainNavigationScaffold(
    viewModel: HifzViewModel,
    isRooted: Boolean,
    onRequestAudioPermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(HifzScreen.Dashboard) }
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val context = LocalContext.current

    var showUpdateDialog by remember { mutableStateOf(false) }
    var latestVersionName by remember { mutableStateOf("") }
    var updateDownloadUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val result = com.example.util.UpdateChecker.getLatestRelease()
            if (result != null) {
                val latestTag = result.first
                val rawTag = latestTag.replace("v", "").replace("V", "").trim()
                val currentVersion = "1.0"
                if (rawTag.isNotEmpty() && rawTag != currentVersion) {
                    latestVersionName = latestTag
                    updateDownloadUrl = result.second
                    showUpdateDialog = true
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Update check failure", e)
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { /* mandatory update, no-op */ },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security, 
                        contentDescription = null, 
                        tint = GoldAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mandatory Update Required", color = Color.White)
                }
            },
            text = {
                Text(
                    text = "A brand new mandatory update ($latestVersionName) is available for HifzGuard on GitHub! You must download and install the update to retrieve the latest improvements and stay guarded before you can continue using the application.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateDownloadUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "No app available to open the link", android.widget.Toast.LENGTH_LONG).show()
                        }
                        // Do not dismiss dialog; wait for the new update to be installed (which restarts the app)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                ) {
                    Text("DOWNLOAD UPDATE", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Periodically verify and ask for overlay permissions if not granted
    val hasOverlayPermission = remember {
        derivedStateOf {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }

    Scaffold(
        bottomBar = {
            // Hide Bottom bar completely during active reading session to lock focus
            if (!isSessionActive) {
                NavigationBar(
                    containerColor = DarkCard,
                    contentColor = Color.White
                ) {
                    NavigationBarItem(
                        selected = currentScreen == HifzScreen.Dashboard,
                        onClick = { currentScreen = HifzScreen.Dashboard },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldAccent,
                            selectedTextColor = GoldAccent,
                            indicatorColor = Color(0x1AD4AF37),
                            unselectedIconColor = Color.White.copy(alpha = 0.5f),
                            unselectedTextColor = Color.White.copy(alpha = 0.5f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == HifzScreen.ReadQuran,
                        onClick = { currentScreen = HifzScreen.ReadQuran },
                        icon = { Icon(Icons.Default.Book, contentDescription = "Read Quran") },
                        label = { Text("Read Quran") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldAccent,
                            selectedTextColor = GoldAccent,
                            indicatorColor = Color(0x1AD4AF37),
                            unselectedIconColor = Color.White.copy(alpha = 0.5f),
                            unselectedTextColor = Color.White.copy(alpha = 0.5f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == HifzScreen.JuzGrid,
                        onClick = { currentScreen = HifzScreen.JuzGrid },
                        icon = { Icon(Icons.Default.GridOn, contentDescription = "Juz Progress") },
                        label = { Text("Juz Progress") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldAccent,
                            selectedTextColor = GoldAccent,
                            indicatorColor = Color(0x1AD4AF37),
                            unselectedIconColor = Color.White.copy(alpha = 0.5f),
                            unselectedTextColor = Color.White.copy(alpha = 0.5f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == HifzScreen.Statistics,
                        onClick = { currentScreen = HifzScreen.Statistics },
                        icon = { Icon(Icons.Default.Analytics, contentDescription = "Statistics") },
                        label = { Text("Statistics") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldAccent,
                            selectedTextColor = GoldAccent,
                            indicatorColor = Color(0x1AD4AF37),
                            unselectedIconColor = Color.White.copy(alpha = 0.5f),
                            unselectedTextColor = Color.White.copy(alpha = 0.5f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen == HifzScreen.Settings,
                        onClick = { currentScreen = HifzScreen.Settings },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GoldAccent,
                            selectedTextColor = GoldAccent,
                            indicatorColor = Color(0x1AD4AF37),
                            unselectedIconColor = Color.White.copy(alpha = 0.5f),
                            unselectedTextColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Dynamic Screen router routing
            when (currentScreen) {
                HifzScreen.Dashboard -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onStartSessionClick = {
                            currentScreen = HifzScreen.Session
                            onRequestAudioPermission()
                        },
                        onNavigateToJuzGrid = {
                            currentScreen = HifzScreen.JuzGrid
                        }
                    )
                }
                HifzScreen.ReadQuran -> {
                    ReadQuranScreen(viewModel = viewModel)
                }
                HifzScreen.Session -> {
                    SessionScreen(
                        viewModel = viewModel,
                        onBackToDashboard = {
                            currentScreen = HifzScreen.Dashboard
                        }
                    )
                }
                HifzScreen.JuzGrid -> {
                    JuzGridScreen(viewModel = viewModel)
                }
                HifzScreen.Statistics -> {
                    StatisticsScreen(viewModel = viewModel)
                }
                HifzScreen.Settings -> {
                    SettingsScreen(viewModel = viewModel)
                }
            }

            // High priority overlay notice bar if locking permission is disabled
            if (!hasOverlayPermission.value && currentScreen != HifzScreen.Session) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFC0392B))
                        .padding(12.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "HifzGuard overlay protection is inactive. Enable drawing permissions to block distractions.",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = onRequestOverlayPermission,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text("CONFIGURE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Layer C: Root Warning Banner (reduced functionality notice)
            if (isRooted && currentScreen == HifzScreen.Dashboard) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    colors = CardDefaults.cardColors(containerColor = Color(0x44D4AF37)),
                    border = BorderStroke(1.dp, GoldAccent)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.HeadsetMic, contentDescription = "Root warning", tint = GoldAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Root Access Detected",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                        Text(
                            text = "HifzGuard security triggers have restricted offline database configurations to safeguard system integrity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TamperedBlockerUI() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1E16))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Tamper Lock",
                tint = Color(0xFFC0392B),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SECURITY PROTECTION VOID",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "This app has been tampered with or modified. Please download the official intact version from HifzGuard to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}
