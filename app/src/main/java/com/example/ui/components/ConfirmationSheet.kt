package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.domain.ConfirmationRequest
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.ObsidianCardElevated
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextSecondary

@Composable
fun SmartConfirmationDialog(
    request: ConfirmationRequest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val accent = if (request.isHighRisk) ErrorRose else CyberCyan
    val icon = when (request) {
        is ConfirmationRequest.PlaceCall -> Icons.Rounded.Call
        is ConfirmationRequest.SendWhatsAppMessage -> Icons.Rounded.Send
        is ConfirmationRequest.ShareMedia -> Icons.Rounded.PhotoLibrary
        is ConfirmationRequest.DeleteFile -> Icons.Rounded.DeleteForever
        is ConfirmationRequest.PaymentGuard -> Icons.Rounded.Security
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianCardElevated,
            border = BorderStroke(1.2.dp, accent.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = accent.copy(alpha = 0.16f)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = request.title,
                            tint = accent,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "SMART CONFIRMATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                        Text(
                            text = request.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = SoftLavender
                        )
                    }
                }

                if (request is ConfirmationRequest.ShareMedia && request.mimeType.startsWith("image")) {
                    AsyncImage(
                        model = request.uri,
                        contentDescription = "Selected photo preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Text(
                    text = request.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_cancel_button"),
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.4f))
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_proceed_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (request.isHighRisk) ErrorRose else ElectricViolet
                        )
                    ) {
                        Text(request.confirmLabel)
                    }
                }
            }
        }
    }
}
