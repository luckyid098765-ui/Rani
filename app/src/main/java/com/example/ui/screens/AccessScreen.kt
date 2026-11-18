package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.preferences.AssistantLanguage
import com.example.data.preferences.RaniSettings
import com.example.phone.DeviceController
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun AccessScreen(
    settings: RaniSettings,
    deviceController: DeviceController,
    accessibilityConnected: Boolean,
    notificationListenerConnected: Boolean,
    wakeWordRunning: Boolean,
    activeVoiceName: String,
    onRequestPermission: (String) -> Unit,
    onOpenSystemSettings: (String) -> Unit,
    onLinkSafFolder: () -> Unit,
    onToggleMic: (Boolean) -> Unit,
    onToggleAutomation: (Boolean) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onSelectLanguage: (AssistantLanguage) -> Unit,
    onUpdateTts: (Float, Float) -> Unit,
    onUpdateConfirmRule: (Boolean?, Boolean?, Boolean?, Boolean?) -> Unit,
    onTogglePrivacyFilter: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hasMic = deviceController.hasPermission(Manifest.permission.RECORD_AUDIO)
    val hasContacts = deviceController.hasPermission(Manifest.permission.READ_CONTACTS)
    val hasCall = deviceController.hasPermission(Manifest.permission.CALL_PHONE)
    val hasCallLog = deviceController.hasPermission(Manifest.permission.READ_CALL_LOG)
    val hasAnswerCalls = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        deviceController.hasPermission(Manifest.permission.ANSWER_PHONE_CALLS)
    } else true
    val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        deviceController.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    } else true
    val canWriteSettings = Settings.System.canWrite(context)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 700.dp)
                .align(Alignment.Center),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text("Access & Controls", style = MaterialTheme.typography.headlineMedium, color = SoftLavender)
                    Text("Permissions, privacy kill-switches, Hindi/English voice & free-tier info", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            // 1. Privacy Kill-Switches & Security
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.PrivacyTip, null, tint = CyberCyan)
                            Text("Privacy & Master Kill-Switches", style = MaterialTheme.typography.titleLarge, color = SoftLavender)
                        }

                        ToggleRow(
                            title = "Microphone Master Switch",
                            subtitle = "Disable anytime to block all voice capture",
                            checked = settings.micEnabled,
                            onCheckedChange = onToggleMic,
                            testTag = "privacy_mic_switch"
                        )
                        ToggleRow(
                            title = "Phone & App Automation",
                            subtitle = "Disable anytime to pause Accessibility & app actions",
                            checked = settings.automationEnabled,
                            onCheckedChange = onToggleAutomation,
                            testTag = "privacy_automation_switch"
                        )
                        ToggleRow(
                            title = "Background \"Hey Rani\" Service",
                            subtitle = "Runs a persistent microphone notification you can stop anytime",
                            checked = wakeWordRunning,
                            onCheckedChange = onToggleWakeWord,
                            testTag = "wake_word_service_switch"
                        )
                        ToggleRow(
                            title = "Notification OTP & Password Shield",
                            subtitle = "Skips OTP, password & PIN notifications from capture",
                            checked = settings.privacyFilterNotifications,
                            onCheckedChange = onTogglePrivacyFilter
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ObsidianCardElevated
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Lock, null, tint = SuccessEmerald)
                                Text(
                                    text = "RANI never reads password inputs, never bypasses app locks/auth, and never executes payments automatically.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Smart Confirmation Rules
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Smart Confirmation Rules", style = MaterialTheme.typography.titleLarge, color = SoftLavender)
                        ToggleRow("Confirm before sending messages", "WhatsApp & compose checks", settings.confirmBeforeMessage) {
                            onUpdateConfirmRule(it, null, null, null)
                        }
                        ToggleRow("Confirm before placing calls", "Phone & WhatsApp call check", settings.confirmBeforeCall) {
                            onUpdateConfirmRule(null, it, null, null)
                        }
                        ToggleRow("Confirm before sharing photos/files", "Media share confirmation", settings.confirmBeforeShare) {
                            onUpdateConfirmRule(null, null, it, null)
                        }
                        ToggleRow("Confirm before deleting files", "SAF deletion guard (Recommended)", settings.confirmBeforeDelete) {
                            onUpdateConfirmRule(null, null, null, it)
                        }
                    }
                }
            }

            // 3. App Permissions Dashboard
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("App Permissions Dashboard", style = MaterialTheme.typography.titleLarge, color = SoftLavender)
                        PermissionRow("Microphone", "Speech-to-text & Wake-word", hasMic) {
                            onRequestPermission(Manifest.permission.RECORD_AUDIO)
                        }
                        PermissionRow("Contacts", "Find names for calls & WhatsApp", hasContacts) {
                            onRequestPermission(Manifest.permission.READ_CONTACTS)
                        }
                        PermissionRow("Phone Calls", "Place voice-initiated calls", hasCall) {
                            onRequestPermission(Manifest.permission.CALL_PHONE)
                        }
                        PermissionRow("Call History", "Open & inspect recent calls", hasCallLog) {
                            onRequestPermission(Manifest.permission.READ_CALL_LOG)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            PermissionRow("Answer / Reject Calls", "Hands-free call control", hasAnswerCalls) {
                                onRequestPermission(Manifest.permission.ANSWER_PHONE_CALLS)
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            PermissionRow("Post Notifications", "Required for Wake-Word service", hasNotifications) {
                                onRequestPermission(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        PermissionRow("Accessibility Service", "Opt-in: Reels scroll, tap & type", accessibilityConnected) {
                            onOpenSystemSettings("accessibility")
                        }
                        PermissionRow("Notification Listener", "Opt-in: Read messages aloud", notificationListenerConnected) {
                            onOpenSystemSettings("notifications")
                        }
                        PermissionRow("System Brightness", "Modify display brightness", canWriteSettings) {
                            onOpenSystemSettings("brightness")
                        }
                        PermissionRow("Photos & Files (SAF)", "Zero-permission Photo Picker & Folder SAF", settings.safFolderUri.isNotBlank()) {
                            onLinkSafFolder()
                        }
                    }
                }
            }

            // 4. Language & Female AI Voice Settings
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Mic, null, tint = CyberCyan)
                            Text("Language & Female Voice", style = MaterialTheme.typography.titleLarge, color = SoftLavender)
                        }

                        AssistantLanguage.entries.forEach { lang ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectLanguage(lang) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.language == lang,
                                    onClick = { onSelectLanguage(lang) }
                                )
                                Column {
                                    Text(lang.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                    Text(lang.displaySub, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                                }
                            }
                        }

                        Text("Active Female Voice: $activeVoiceName", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                        Text("Speech Rate: ${"%.2f".format(settings.ttsSpeechRate)}x", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Slider(
                            value = settings.ttsSpeechRate,
                            onValueChange = { onUpdateTts(it, settings.ttsPitch) },
                            valueRange = 0.7f..1.4f
                        )
                        Text("Female Voice Pitch: ${"%.2f".format(settings.ttsPitch)}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Slider(
                            value = settings.ttsPitch,
                            onValueChange = { onUpdateTts(settings.ttsSpeechRate, it) },
                            valueRange = 0.85f..1.35f
                        )
                    }
                }
            }

            // 5. Free Version & API Cost Transparency
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = WarningAmber.copy(alpha = 0.45f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Info, null, tint = WarningAmber)
                            Text("Free Version & API Transparency", style = MaterialTheme.typography.titleLarge, color = SoftLavender)
                        }
                        Text(
                            text = "• 100% Free On-Device: Android SpeechRecognizer, Female Text-to-Speech, App Launcher, WhatsApp/Social Intents, Accessibility Automation, Contacts, Calling, Alarms, Volume/Media, and SAF File Search/Delete.\n" +
                                "• No Mandatory Paid Subscription.\n" +
                                "• Cloud AI & Image Generation Limits: Live Gemini 3.8 Flash / 3.5 Flash-Lite (free tier quota) and Gemini Nano Banana 2.1 image generation (Blaze pay-as-you-go plan) require Firebase AI Logic to be enabled; currently running in sample placeholder mode.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianCardElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.AccessibilityNew,
                        contentDescription = null,
                        tint = if (granted) SuccessEmerald else WarningAmber
                    )
                    Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            }
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (granted) SuccessEmerald.copy(alpha = 0.2f) else ElectricViolet
                )
            ) {
                Text(if (granted) "Active" else "Enable", color = if (granted) SuccessEmerald else TextPrimary)
            }
        }
    }
}
