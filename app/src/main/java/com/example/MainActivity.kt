package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.RaniUiEvent
import com.example.ui.RaniViewModel
import com.example.ui.components.SmartConfirmationDialog
import com.example.ui.screens.AccessScreen
import com.example.ui.screens.ActionsScreen
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.RaniAiTheme
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted

const val ROUTE_RANI = "rani"
const val ROUTE_ACTIONS = "actions"
const val ROUTE_ACTIVITY = "activity"
const val ROUTE_ACCESS = "access"

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {

    private var activeViewModel: RaniViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RaniAiTheme {
                val vm: RaniViewModel = viewModel()
                activeViewModel = vm
                LaunchedEffect(Unit) {
                    vm.handleIncomingIntent(intent)
                }
                RaniAppShell(viewModel = vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        activeViewModel?.handleIncomingIntent(intent)
    }
}

@Composable
private fun RaniAppShell(viewModel: RaniViewModel) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val orbState by viewModel.orbState.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.rmsLevel.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val wakeWordRunning by viewModel.wakeWordServiceRunning.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val accessibilityConnected by viewModel.accessibilityConnected.collectAsStateWithLifecycle()
    val notificationConnected by viewModel.notificationListenerConnected.collectAsStateWithLifecycle()
    val capturedNotifications by viewModel.capturedNotifications.collectAsStateWithLifecycle()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsStateWithLifecycle()
    val contacts by viewModel.contactSearchResults.collectAsStateWithLifecycle()
    val safFiles by viewModel.safSearchResults.collectAsStateWithLifecycle()
    val imagePreview by viewModel.latestImagePreview.collectAsStateWithLifecycle()
    val activeVoiceName by viewModel.activeVoiceName.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // Inform user politely without forcing
            viewModel.interruptVoice()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhotoPicked(uri)
        }
    }

    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onDocumentPickedForShare(uri)
        }
    }

    val safFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            }
            viewModel.onSafFolderSelected(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is RaniUiEvent.RequestPermission -> {
                    permissionLauncher.launch(event.permission)
                }

                is RaniUiEvent.LaunchPhotoPicker -> {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                }

                is RaniUiEvent.LaunchDocumentPickerForShare -> {
                    docPickerLauncher.launch(arrayOf("*/*"))
                }

                is RaniUiEvent.LaunchSafFolderPicker -> {
                    safFolderLauncher.launch(null)
                }

                is RaniUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val tabs = remember {
        listOf(
            NavTab(ROUTE_RANI, "Rani", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome),
            NavTab(ROUTE_ACTIONS, "Actions", Icons.Rounded.Bolt, Icons.Outlined.Bolt),
            NavTab(ROUTE_ACTIVITY, "Activity", Icons.Rounded.History, Icons.Outlined.History),
            NavTab(ROUTE_ACCESS, "Access", Icons.Rounded.Shield, Icons.Outlined.Shield)
        )
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navigateTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianVoid,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = ObsidianSurface) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyberCyan,
                                    selectedTextColor = SoftLavender,
                                    indicatorColor = ElectricViolet.copy(alpha = 0.28f),
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(containerColor = ObsidianCard) {
                        tabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_RANI,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(ROUTE_RANI) {
                        HomeScreen(
                            orbState = orbState,
                            rmsLevel = rmsLevel,
                            partialTranscript = partialTranscript,
                            settings = settings,
                            wakeWordRunning = wakeWordRunning,
                            chatMessages = chatMessages,
                            onTapOrb = { viewModel.startVoiceListening() },
                            onInterrupt = { viewModel.interruptVoice() },
                            onToggleContinuous = { viewModel.toggleContinuousMode(it) },
                            onToggleWakeWord = { viewModel.toggleWakeWordBackground(it) },
                            onSendCommand = { viewModel.handleUserInput(it, fromVoice = false) }
                        )
                    }

                    composable(ROUTE_ACTIONS) {
                        ActionsScreen(
                            accessibilityConnected = accessibilityConnected,
                            safFolderLinked = settings.safFolderUri.isNotBlank(),
                            contacts = contacts,
                            safFiles = safFiles,
                            imagePreview = imagePreview,
                            onRunCommand = {
                                viewModel.handleUserInput(it, fromVoice = false)
                                navigateTab(ROUTE_RANI)
                            },
                            onOpenApp = { app ->
                                val res = viewModel.deviceController.openSupportedApp(app)
                                if (!res.success) {
                                    viewModel.handleUserInput("open ${app.displayName}", fromVoice = false)
                                }
                            },
                            onSearchContacts = { viewModel.searchContactsNow(it) },
                            onDraftWhatsApp = { rec, topic -> viewModel.draftMessageTool(rec, topic) },
                            onPickPhoto = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            onPickDocument = { docPickerLauncher.launch(arrayOf("*/*")) },
                            onLinkSafFolder = { safFolderLauncher.launch(null) },
                            onSearchSafFiles = { viewModel.searchSafFilesNow(it) },
                            onOpenSafFile = { file ->
                                viewModel.deviceController.openSafDocument(file.uri, file.mimeType)
                            },
                            onDeleteSafFile = { file -> viewModel.requestDeleteSafFile(file) }
                        )
                    }

                    composable(ROUTE_ACTIVITY) {
                        ActivityScreen(
                            activityLogs = activityLogs,
                            memories = memories,
                            notifications = capturedNotifications,
                            onAddMemory = { k, v -> viewModel.addManualMemory(k, v) },
                            onDeleteMemory = { viewModel.deleteMemory(it) },
                            onClearHistory = { viewModel.clearHistoryAndChat() },
                            onReadNotifications = {
                                viewModel.handleUserInput("read notifications", fromVoice = true)
                                navigateTab(ROUTE_RANI)
                            }
                        )
                    }

                    composable(ROUTE_ACCESS) {
                        AccessScreen(
                            settings = settings,
                            deviceController = viewModel.deviceController,
                            accessibilityConnected = accessibilityConnected,
                            notificationListenerConnected = notificationConnected,
                            wakeWordRunning = wakeWordRunning,
                            activeVoiceName = activeVoiceName,
                            onRequestPermission = { perm -> permissionLauncher.launch(perm) },
                            onOpenSystemSettings = { section ->
                                viewModel.deviceController.openSystemSettings(section)
                            },
                            onLinkSafFolder = { safFolderLauncher.launch(null) },
                            onToggleMic = { viewModel.toggleMicEnabled(it) },
                            onToggleAutomation = { viewModel.toggleAutomationEnabled(it) },
                            onToggleWakeWord = { viewModel.toggleWakeWordBackground(it) },
                            onSelectLanguage = { viewModel.setLanguage(it) },
                            onUpdateTts = { rate, pitch -> viewModel.updateTtsParams(rate, pitch) },
                            onUpdateConfirmRule = { m, c, s, d ->
                                viewModel.updateConfirmationRule(m, c, s, d)
                            },
                            onTogglePrivacyFilter = { viewModel.setPrivacyFilterNotifications(it) }
                        )
                    }
                }
            }
        }
    }

    pendingConfirmation?.let { req ->
        SmartConfirmationDialog(
            request = req,
            onConfirm = { viewModel.confirmPendingAction() },
            onDismiss = { viewModel.dismissPendingAction() }
        )
    }
}
