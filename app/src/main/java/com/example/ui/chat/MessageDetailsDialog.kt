package com.example.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.data.local.entity.MessageType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageDetailsDialog(
    message: MessageEntity,
    contactName: String?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val fullDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm:ss a", Locale.getDefault())
    val formattedDate = try {
        fullDateFormat.format(Date(message.timestamp))
    } catch (_: Exception) {
        message.timestamp.toString()
    }

    val isFromMe = message.senderPhoneNumber == "ME"
    val isMms = message.messageType == MessageType.MMS || !message.mediaUrl.isNullOrBlank()
    val typeLabel = if (isMms) "Multimedia Message (MMS)" else "Text Message (SMS)"

    val statusText = when (message.status) {
        MessageStatus.DELIVERED -> "Delivered"
        MessageStatus.SENT -> "Sent"
        MessageStatus.SENDING -> "Sending..."
        MessageStatus.FAILED -> "Failed to send"
        MessageStatus.READ -> "Read"
        MessageStatus.CANCELLED -> "Cancelled"
    }

    val charCount = message.content.length
    val segmentCount = if (charCount <= 160) 1 else (charCount + 152) / 153

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Message details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Detail Rows
                DetailItem(label = "Type", value = typeLabel)
                DetailItem(label = "Status", value = statusText)
                DetailItem(label = "Date & Time", value = formattedDate)

                if (isFromMe) {
                    DetailItem(label = "From", value = "You (Me)")
                    DetailItem(label = "To", value = contactName?.let { "$it (${message.recipientPhoneNumber})" } ?: message.recipientPhoneNumber)
                } else {
                    DetailItem(label = "From", value = contactName?.let { "$it (${message.senderPhoneNumber})" } ?: message.senderPhoneNumber)
                    DetailItem(label = "To", value = "You (Me)")
                }

                if (!isMms && charCount > 0) {
                    DetailItem(label = "Size", value = "$charCount characters (${segmentCount} SMS segment${if (segmentCount > 1) "s" else ""})")
                }

                if (!message.mediaUrl.isNullOrBlank()) {
                    DetailItem(label = "Attachment", value = message.mediaUrl)
                }

                DetailItem(label = "Message ID", value = message.messageId)

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val detailsSummary = buildString {
                                appendLine("Type: $typeLabel")
                                appendLine("Status: $statusText")
                                appendLine("Date: $formattedDate")
                                appendLine("Content: ${message.content}")
                                appendLine("ID: ${message.messageId}")
                            }
                            clipboard.setPrimaryClip(ClipData.newPlainText("Message Details", detailsSummary))
                            Toast.makeText(context, "Details copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy", maxLines = 1)
                    }

                    Button(
                        onClick = onDismiss,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
