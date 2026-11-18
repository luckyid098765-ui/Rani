package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ChatMessageEntity
import com.example.data.preferences.RaniSettings
import com.example.ui.components.GlassCard
import com.example.ui.components.RaniOrb
import com.example.ui.components.VoiceWaveform
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepRoyalPurple
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonLilac
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.voice.VoiceOrbState

@Composable
fun HomeScreen(
    orbState: VoiceOrbState,
    rmsLevel: Float,
    partialTranscript: String,
    settings: RaniSettings,
    wakeWordRunning: Boolean,
    chatMessages: List<ChatMessageEntity>,
    onTapOrb: () -> Unit,
    onInterrupt: () -> Unit,
    onToggleContinuous: (Boolean) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onSendCommand: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.lastIndex)
        }
    }

    val quickPrompts = remember {
        listOf(
            "Open YouTube",
            "Open chat Family",
            "Instagram scroll reels",
            "Latest photo Rahul ko bhejo",
            "Volume up",
            "Set alarm 7 am",
            "Find file invoice",
            "Summarize"
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepRoyalPurple.copy(alpha = 0.28f), ObsidianVoid, ObsidianVoid)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 700.dp)
                .align(Alignment.Center)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Top Brand Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium,
                        color = SoftLavender
                    )
                    Text(
                        text = stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan
                    )
                }

                if (orbState == VoiceOrbState.SPEAKING || orbState == VoiceOrbState.LISTENING) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = ObsidianCardElevated,
                        border = BorderStroke(1.dp, NeonLilac.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .clickable(onClick = onInterrupt)
                            .testTag("interrupt_voice_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.StopCircle,
                                contentDescription = "Interrupt RANI",
                                tint = NeonLilac,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Interrupt", style = MaterialTheme.typography.labelLarge, color = SoftLavender)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Orb + Waveform
            RaniOrb(
                orbState = orbState,
                rmsLevel = rmsLevel,
                wakeWordActive = wakeWordRunning,
                onTapOrb = onTapOrb,
                size = 164.dp
            )

            Spacer(modifier = Modifier.height(6.dp))

            VoiceWaveform(
                active = orbState == VoiceOrbState.LISTENING || orbState == VoiceOrbState.SPEAKING,
                rmsLevel = rmsLevel
            )

            if (partialTranscript.isNotBlank()) {
                Text(
                    text = "\"$partialTranscript\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberCyan,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Mode Toggles Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = wakeWordRunning,
                    onClick = { onToggleWakeWord(!wakeWordRunning) },
                    label = { Text("Hey Rani BG") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Hearing,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                        selectedLabelColor = CyberCyan,
                        selectedLeadingIconColor = CyberCyan
                    ),
                    modifier = Modifier.testTag("wake_word_chip")
                )
                FilterChip(
                    selected = settings.continuousConversation,
                    onClick = { onToggleContinuous(!settings.continuousConversation) },
                    label = { Text("Continuous") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricViolet.copy(alpha = 0.28f),
                        selectedLabelColor = SoftLavender,
                        selectedLeadingIconColor = SoftLavender
                    ),
                    modifier = Modifier.testTag("continuous_mode_chip")
                )
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text(settings.language.label) }
                )
            }

            // Quick Bilingual Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPrompts.forEach { cmd ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ObsidianCard,
                        border = BorderStroke(1.dp, SubtleBorder),
                        modifier = Modifier.clickable { onSendCommand(cmd) }
                    ) {
                        Text(
                            text = cmd,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chat History Feed
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }
            }

            // Bottom Command Composer + Mic Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            "Speak or type in Hindi / English / Hinglish...",
                            color = TextMuted,
                            maxLines = 1
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonLilac,
                        unfocusedBorderColor = SubtleBorder,
                        focusedContainerColor = ObsidianCard,
                        unfocusedContainerColor = ObsidianCard
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (inputText.isNotBlank()) {
                            onSendCommand(inputText)
                            inputText = ""
                        }
                    }),
                    trailingIcon = {
                        if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onSendCommand(inputText)
                                    inputText = ""
                                },
                                modifier = Modifier.testTag("send_command_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "Send command",
                                    tint = CyberCyan
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("command_input")
                )

                Surface(
                    shape = CircleShape,
                    color = if (!settings.micEnabled) {
                        ObsidianCard
                    } else if (orbState == VoiceOrbState.LISTENING) {
                        CyberCyan
                    } else {
                        ElectricViolet
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clickable(onClick = onTapOrb)
                        .testTag("mic_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (settings.micEnabled) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                            contentDescription = "Voice Mic",
                            tint = if (orbState == VoiceOrbState.LISTENING) ObsidianVoid else TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessageEntity) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        GlassCard(
            modifier = Modifier.widthIn(max = 310.dp),
            borderColor = if (isUser) ElectricViolet.copy(alpha = 0.65f) else SubtleBorder
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isUser) "YOU" else "RANI AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) NeonLilac else CyberCyan
                    )
                    if (!message.actionBadge.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricViolet.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = message.actionBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftLavender,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )

                if (message.isPlaceholderAi) {
                    Text(
                        text = stringResource(R.string.ai_firebase_notice),
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber
                    )
                }
            }
        }
    }
}
