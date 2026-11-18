package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.SwipeUp
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.AiImagePreview
import com.example.phone.ContactMatch
import com.example.phone.DeviceController.SupportedApp
import com.example.phone.SafFileEntry
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.NeonLilac
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionsScreen(
    accessibilityConnected: Boolean,
    safFolderLinked: Boolean,
    contacts: List<ContactMatch>,
    safFiles: List<SafFileEntry>,
    imagePreview: AiImagePreview?,
    onRunCommand: (String) -> Unit,
    onOpenApp: (SupportedApp) -> Unit,
    onSearchContacts: (String) -> Unit,
    onDraftWhatsApp: (String, String) -> Unit,
    onPickPhoto: () -> Unit,
    onPickDocument: () -> Unit,
    onLinkSafFolder: () -> Unit,
    onSearchSafFiles: (String) -> Unit,
    onOpenSafFile: (SafFileEntry) -> Unit,
    onDeleteSafFile: (SafFileEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var chatSearchName by remember { mutableStateOf("Family") }
    var waRecipient by remember { mutableStateOf("") }
    var waTopic by remember { mutableStateOf("") }
    var contactQuery by remember { mutableStateOf("") }
    var fileQuery by remember { mutableStateOf("invoice") }
    var typeTextInput by remember { mutableStateOf("hello") }
    var aiImagePrompt by remember { mutableStateOf("Futuristic purple cyber lotus") }

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
            // Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clip(RoundedCornerShape(22.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_rani_banner_1794965216114),
                        contentDescription = "RANI AI Control Deck Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ObsidianVoid.copy(alpha = 0.88f), Color.Transparent)
                                )
                            )
                            .padding(18.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text("AUTOMATION DECK", style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                            Text("Apps, Calls, Files & AI", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                            Text(
                                text = if (accessibilityConnected) "Accessibility Active" else "Accessibility Optional (Enable in Access)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SoftLavender
                            )
                        }
                    }
                }
            }

            // 1. Social Media Control Deck
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Social Media & Web Apps", style = MaterialTheme.typography.titleLarge, color = SoftLavender)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SupportedApp.entries.forEach { app ->
                                Button(
                                    onClick = { onOpenApp(app) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianCardElevated),
                                    border = BorderStroke(1.dp, SubtleBorder),
                                    modifier = Modifier.testTag("open_app_${app.name.lowercase()}")
                                ) {
                                    Text(app.displayName, color = TextPrimary)
                                }
                            }
                        }

                        Text("In-App Accessibility Remote (Reels / Shorts / Feeds)", style = MaterialTheme.typography.labelLarge, color = CyberCyan)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { onRunCommand("scroll reels") },
                                label = { Text("Scroll Next") },
                                leadingIcon = { Icon(Icons.Rounded.SwipeUp, null, Modifier.size(16.dp)) }
                            )
                            AssistChip(
                                onClick = { onRunCommand("like") },
                                label = { Text("Like") },
                                leadingIcon = { Icon(Icons.Rounded.ThumbUp, null, Modifier.size(16.dp)) }
                            )
                            AssistChip(onClick = { onRunCommand("comment") }, label = { Text("Comment") })
                            AssistChip(onClick = { onRunCommand("share") }, label = { Text("Share") })
                            AssistChip(onClick = { onRunCommand("follow") }, label = { Text("Follow") })
                            AssistChip(onClick = { onRunCommand("subscribe") }, label = { Text("Subscribe") })
                            AssistChip(onClick = { onRunCommand("read screen") }, label = { Text("Read Screen") })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = typeTextInput,
                                onValueChange = { typeTextInput = it },
                                label = { Text("Type text in active app") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onRunCommand("type $typeTextInput") },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                            ) {
                                Text("Type")
                            }
                        }
                    }
                }
            }

            // 2. WhatsApp Assistant
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Chat, null, tint = CyberCyan)
                            Text("WhatsApp Assistant", style = MaterialTheme.typography.titleLarge, color = SoftLavender)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = chatSearchName,
                                onValueChange = { chatSearchName = it },
                                label = { Text("Chat / Group Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onRunCommand("open chat $chatSearchName") },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                modifier = Modifier.testTag("open_whatsapp_chat_button")
                            ) {
                                Text("Open Chat")
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = waRecipient,
                                onValueChange = { waRecipient = it },
                                label = { Text("Contact") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = waTopic,
                                onValueChange = { waTopic = it },
                                label = { Text("Message / Topic") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onDraftWhatsApp(waRecipient, waTopic) },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                            ) {
                                Text("Compose & Confirm")
                            }
                            OutlinedButton(onClick = onPickPhoto, modifier = Modifier.testTag("pick_photo_button")) {
                                Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Share Photo")
                            }
                            OutlinedButton(onClick = onPickDocument, modifier = Modifier.testTag("pick_doc_button")) {
                                Icon(Icons.Rounded.Description, null, Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text("Share Document")
                            }
                        }
                    }
                }
            }

            // 3. Phone Calling & Contacts
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Phone Calling & Contacts", style = MaterialTheme.typography.titleLarge, color = SoftLavender)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = contactQuery,
                                onValueChange = { contactQuery = it },
                                label = { Text("Search Contact or Number") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = { onSearchContacts(contactQuery) }) {
                                Icon(Icons.Rounded.Search, contentDescription = "Search contacts")
                            }
                        }

                        if (contacts.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                contacts.take(5).forEach { c ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = ObsidianCardElevated,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onRunCommand("call ${c.name}") }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(c.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                                Text(c.phoneNumber, style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                            }
                                            Icon(Icons.Rounded.Call, contentDescription = "Call ${c.name}", tint = CyberCyan)
                                        }
                                    }
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { onRunCommand("answer call") },
                                label = { Text("Answer Call") },
                                leadingIcon = { Icon(Icons.Rounded.Call, null, Modifier.size(16.dp)) }
                            )
                            AssistChip(
                                onClick = { onRunCommand("reject call") },
                                label = { Text("End / Reject") },
                                leadingIcon = { Icon(Icons.Rounded.CallEnd, null, Modifier.size(16.dp)) }
                            )
                            AssistChip(
                                onClick = { onRunCommand("open call history") },
                                label = { Text("Call History") },
                                leadingIcon = { Icon(Icons.Rounded.History, null, Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }

            // 4. Device Automation & SAF Files
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Phone Controls & SAF File Manager", style = MaterialTheme.typography.titleLarge, color = SoftLavender)

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(onClick = { onRunCommand("volume up") }, label = { Text("Vol +") }, leadingIcon = { Icon(Icons.Rounded.VolumeUp, null, Modifier.size(16.dp)) })
                            AssistChip(onClick = { onRunCommand("volume down") }, label = { Text("Vol -") }, leadingIcon = { Icon(Icons.Rounded.VolumeDown, null, Modifier.size(16.dp)) })
                            AssistChip(onClick = { onRunCommand("mute") }, label = { Text("Mute") }, leadingIcon = { Icon(Icons.Rounded.VolumeOff, null, Modifier.size(16.dp)) })
                            AssistChip(onClick = { onRunCommand("brightness up") }, label = { Text("Bright +") }, leadingIcon = { Icon(Icons.Rounded.BrightnessHigh, null, Modifier.size(16.dp)) })
                            AssistChip(onClick = { onRunCommand("brightness down") }, label = { Text("Bright -") }, leadingIcon = { Icon(Icons.Rounded.BrightnessLow, null, Modifier.size(16.dp)) })
                            AssistChip(onClick = { onRunCommand("pause music") }, label = { Text("Play/Pause") }, leadingIcon = { Icon(Icons.Rounded.PlayArrow, null, Modifier.size(16.dp)) })
                            AssistChip(onClick = { onRunCommand("next song") }, label = { Text("Next") }, leadingIcon = { Icon(Icons.Rounded.SkipNext, null, Modifier.size(16.dp)) })
                            AssistChip(onClick = { onRunCommand("previous song") }, label = { Text("Prev") }, leadingIcon = { Icon(Icons.Rounded.SkipPrevious, null, Modifier.size(16.dp)) })
                            AssistChip(onClick = { onRunCommand("set alarm 7 am") }, label = { Text("Alarm 7 AM") }, leadingIcon = { Icon(Icons.Rounded.Alarm, null, Modifier.size(16.dp)) })
                            AssistChip(onClick = { onRunCommand("set timer 5 minutes") }, label = { Text("Timer 5m") }, leadingIcon = { Icon(Icons.Rounded.MusicNote, null, Modifier.size(16.dp)) })
                            AssistChip(onClick = { onRunCommand("open settings") }, label = { Text("Settings") }, leadingIcon = { Icon(Icons.Rounded.Settings, null, Modifier.size(16.dp)) })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onLinkSafFolder,
                                modifier = Modifier.testTag("link_saf_folder_button")
                            ) {
                                Icon(Icons.Rounded.FolderOpen, null, Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(if (safFolderLinked) "Change Folder" else "Link Folder (SAF)")
                            }
                            OutlinedTextField(
                                value = fileQuery,
                                onValueChange = { fileQuery = it },
                                label = { Text("Find file") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = { onSearchSafFiles(fileQuery) }) {
                                Text("Find")
                            }
                        }

                        if (safFiles.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                safFiles.take(6).forEach { file ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = ObsidianCardElevated,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(file.displayName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1)
                                                Text(file.mimeType, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            }
                                            IconButton(onClick = { onOpenSafFile(file) }) {
                                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open ${file.displayName}", tint = CyberCyan)
                                            }
                                            IconButton(onClick = { onDeleteSafFile(file) }) {
                                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete ${file.displayName}", tint = ErrorRose)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. AI Intelligence Tools
            item {
                GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = WarningAmber.copy(alpha = 0.45f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("AI Intelligence Studio", style = MaterialTheme.typography.titleLarge, color = SoftLavender)
                        Text(
                            text = stringResourceCompatNotice(),
                            style = MaterialTheme.typography.labelMedium,
                            color = WarningAmber
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { onRunCommand("summarize") },
                                label = { Text("Summarize Clipboard") },
                                leadingIcon = { Icon(Icons.Rounded.Summarize, null, Modifier.size(16.dp)) }
                            )
                            AssistChip(
                                onClick = { onRunCommand("translate Hello how are you to Hindi") },
                                label = { Text("Translate Sample") },
                                leadingIcon = { Icon(Icons.Rounded.Translate, null, Modifier.size(16.dp)) }
                            )
                            AssistChip(
                                onClick = { onRunCommand("write kotlin coroutine example") },
                                label = { Text("Coding & Study") }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = aiImagePrompt,
                                onValueChange = { aiImagePrompt = it },
                                label = { Text("Image Gen Prompt") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = { onRunCommand("generate image of $aiImagePrompt") },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                            ) {
                                Icon(Icons.Rounded.Image, contentDescription = "Generate image")
                            }
                        }

                        if (imagePreview != null) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = ObsidianCardElevated
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(imagePreview.summary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                    Text(imagePreview.notice, style = MaterialTheme.typography.labelSmall, color = WarningAmber)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun stringResourceCompatNotice(): String =
    "Placeholder AI active: Enable Firebase AI Logic in AI Studio to switch Q&A, translation, summarization & Gemini Nano Banana image generation to live Gemini."
