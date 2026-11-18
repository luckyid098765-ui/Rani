package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.automation.CapturedNotification
import com.example.data.local.ActivityLogEntity
import com.example.data.local.MemoryPreferenceEntity
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen(
    activityLogs: List<ActivityLogEntity>,
    memories: List<MemoryPreferenceEntity>,
    notifications: List<CapturedNotification>,
    onAddMemory: (String, String) -> Unit,
    onDeleteMemory: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onReadNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    var memoryKey by remember { mutableStateOf("") }
    var memoryValue by remember { mutableStateOf("") }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    val filters = listOf("All", "Phone", "WhatsApp", "Accessibility", "Files", "AI")
    val filteredLogs = remember(activityLogs, selectedFilter) {
        if (selectedFilter == "All") activityLogs
        else activityLogs.filter {
            it.category.contains(selectedFilter, ignoreCase = true) ||
                it.status.contains(selectedFilter, ignoreCase = true)
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Activity & Memory", style = MaterialTheme.typography.headlineMedium, color = SoftLavender)
                        Text("Recent commands, notifications & saved preferences", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    OutlinedButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear history")
                    }
                }
            }

            // Notifications Preview Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.NotificationsActive, null, tint = CyberCyan)
                                Text("Captured Notifications (${notifications.size})", style = MaterialTheme.typography.titleMedium, color = SoftLavender)
                            }
                            Button(
                                onClick = onReadNotifications,
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                            ) {
                                Text("Read Aloud")
                            }
                        }

                        if (notifications.isEmpty()) {
                            Text(
                                text = "No notifications captured yet. Enable Notification Access in the Access tab when needed.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        } else {
                            notifications.take(4).forEach { notif ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ObsidianCardElevated,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("[${notif.appLabel}] ${notif.title}", style = MaterialTheme.typography.labelLarge, color = CyberCyan)
                                        Text(notif.text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // User-Approved Preferences & Memory
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.BookmarkAdded, null, tint = SoftLavender)
                            Text("User-Approved Preferences", style = MaterialTheme.typography.titleMedium, color = SoftLavender)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = memoryKey,
                                onValueChange = { memoryKey = it },
                                label = { Text("Label (e.g. City)") },
                                singleLine = true,
                                modifier = Modifier.weight(0.42f)
                            )
                            OutlinedTextField(
                                value = memoryValue,
                                onValueChange = { memoryValue = it },
                                label = { Text("Preference") },
                                singleLine = true,
                                modifier = Modifier.weight(0.58f)
                            )
                            IconButton(
                                onClick = {
                                    onAddMemory(memoryKey, memoryValue)
                                    memoryKey = ""
                                    memoryValue = ""
                                },
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Save preference", tint = CyberCyan)
                            }
                        }

                        if (memories.isEmpty()) {
                            Text(
                                text = "No saved preferences yet. Say \"Remember that I prefer Hinglish\" or add one above.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        } else {
                            memories.forEach { mem ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = ObsidianCardElevated,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(mem.prefKey, style = MaterialTheme.typography.labelMedium, color = CyberCyan)
                                            Text(mem.prefValue, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                        }
                                        IconButton(onClick = { onDeleteMemory(mem.id) }) {
                                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Forget preference", tint = ErrorRose)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Activity Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { f ->
                        FilterChip(
                            selected = selectedFilter == f,
                            onClick = { selectedFilter = f },
                            label = { Text(f) }
                        )
                    }
                }
            }

            if (filteredLogs.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No activity logs match \"$selectedFilter\" yet. Speak or run any action to see live history.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(filteredLogs, key = { it.id }) { log ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${log.category.uppercase()} • ${log.status}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (log.status == "Executed") SuccessEmerald else CyberCyan
                                )
                                Text(
                                    text = timeFormatter.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            Text(log.command, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(log.resultSummary, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
