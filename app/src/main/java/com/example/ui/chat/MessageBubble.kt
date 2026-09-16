package com.example.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.ui.util.PhoneNumberUtil
import com.example.ui.util.TimeFormatter

/**
 * Pure SMS Message Bubble strictly adhering to the Human-Coded flat design:
 * - Sent: Primary blue #1688F5 with crisp white text
 * - Received: Cards/containers #0B1424 (Dark) / #FFFFFF (Light) with subtle border
 * - Zero gradients, clear typography, and rich SMS ergonomics (OTP chip, Delivery marks, Reactions).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    isFromMe: Boolean,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    reactionEmoji: String?,
    bubbleShape: String = "ROUNDED",
    fontSize: String = "NORMAL",
    showDeliveryMarks: Boolean = true,
    customColorHex: String? = null,
    highlightQuery: String = "",
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit,
    onRetryClick: () -> Unit,
    onMediaClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val outerRadius = when (bubbleShape) {
        "PILL" -> 24.dp
        "SQUARE" -> 16.dp
        else -> 20.dp
    }
    val innerRadius = when (bubbleShape) {
        "PILL" -> 8.dp
        "SQUARE" -> 6.dp
        else -> 6.dp
    }

    val shape = if (isFromMe) {
        RoundedCornerShape(
            topStart = outerRadius,
            topEnd = if (isFirstInGroup) outerRadius else innerRadius,
            bottomEnd = if (isLastInGroup) outerRadius else innerRadius,
            bottomStart = outerRadius
        )
    } else {
        RoundedCornerShape(
            topStart = if (isFirstInGroup) outerRadius else innerRadius,
            topEnd = outerRadius,
            bottomEnd = outerRadius,
            bottomStart = if (isLastInGroup) outerRadius else innerRadius
        )
    }

    val customColor = customColorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null }
    }

    val backgroundColor = when {
        isFromMe -> customColor ?: MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        isFromMe -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val textStyle = when (fontSize) {
        "SMALL" -> MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp)
        "LARGE" -> MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 23.sp)
        "EXTRA_LARGE" -> MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, lineHeight = 25.sp)
        else -> MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp)
    }

    val senderLabel = if (isFromMe) "You" else "Contact"
    val statusLabel = when (message.status) {
        MessageStatus.DELIVERED -> "Delivered"
        MessageStatus.SENT -> "Sent"
        MessageStatus.SENDING -> "Sending"
        MessageStatus.FAILED -> "Failed to send"
        MessageStatus.READ -> "Read"
        MessageStatus.CANCELLED -> "Cancelled"
    }
    val accessibilityDescription = "$senderLabel at ${TimeFormatter.formatMessageTimestamp(message.timestamp)}: ${message.content}. Status: $statusLabel."

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isSelectionMode) { onClick() }
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = if (isFirstInGroup) 3.dp else 1.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection Checkmark indicator on Left
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .then(
                        if (!isSelected) Modifier.background(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            CircleShape
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = shape,
                color = backgroundColor,
                contentColor = contentColor,
                border = if (!isFromMe) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
                tonalElevation = if (isFromMe) 0.dp else 1.dp,
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .semantics { contentDescription = accessibilityDescription }
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
                    .testTag("message_bubble_${message.messageId}")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // SMS Text Content with Search Highlighting
                    if (message.content.isNotBlank()) {
                        SelectionContainer {
                            if (highlightQuery.isNotBlank() && message.content.contains(highlightQuery, ignoreCase = true)) {
                                val annotatedString = buildAnnotatedString {
                                    val text = message.content
                                    var startIndex = 0
                                    while (startIndex < text.length) {
                                        val matchIndex = text.indexOf(highlightQuery, startIndex, ignoreCase = true)
                                        if (matchIndex == -1) {
                                            append(text.substring(startIndex))
                                            break
                                        }
                                        append(text.substring(startIndex, matchIndex))
                                        withStyle(
                                            style = SpanStyle(
                                                background = Color(0xFFFDE047),
                                                color = Color(0xFF1E293B),
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) {
                                            append(text.substring(matchIndex, matchIndex + highlightQuery.length))
                                        }
                                        startIndex = matchIndex + highlightQuery.length
                                    }
                                }
                                Text(
                                    text = annotatedString,
                                    style = textStyle,
                                    color = contentColor
                                )
                            } else {
                                Text(
                                    text = message.content,
                                    style = textStyle,
                                    color = contentColor
                                )
                            }
                        }
                    }

                    // Smart OTP / Verification Code Quick-Copy Chip
                    val detectedOtp = remember(message.content, isFromMe) {
                        if (!isFromMe) PhoneNumberUtil.extractOtpCode(message.content) else null
                    }
                    if (detectedOtp != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(detectedOtp))
                                    Toast.makeText(context, "Copied code $detectedOtp to clipboard", Toast.LENGTH_SHORT).show()
                                }
                                .testTag("otp_copy_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy code",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Copy code: $detectedOtp",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Timestamp + Status Checkmarks
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = TimeFormatter.formatMessageTimestamp(message.timestamp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = if (isFromMe) contentColor.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isFromMe) {
                            Spacer(modifier = Modifier.width(4.dp))
                            when (message.status) {
                                MessageStatus.SENDING -> {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Sending",
                                        modifier = Modifier.size(11.dp),
                                        tint = contentColor.copy(alpha = 0.7f)
                                    )
                                }
                                MessageStatus.SENT -> {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Sent",
                                        modifier = Modifier.size(12.dp),
                                        tint = contentColor.copy(alpha = 0.8f)
                                    )
                                }
                                MessageStatus.DELIVERED -> {
                                    if (showDeliveryMarks) {
                                        Icon(
                                            imageVector = Icons.Default.DoneAll,
                                            contentDescription = "Delivered",
                                            modifier = Modifier.size(13.dp),
                                            tint = contentColor
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Sent",
                                            modifier = Modifier.size(12.dp),
                                            tint = contentColor.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                                MessageStatus.READ -> {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Read",
                                        modifier = Modifier.size(13.dp),
                                        tint = contentColor
                                    )
                                }
                                MessageStatus.FAILED -> {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Failed",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                MessageStatus.CANCELLED -> {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Cancelled",
                                        modifier = Modifier.size(13.dp),
                                        tint = contentColor.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Emoji Reaction pill
            if (!reactionEmoji.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .align(if (isFromMe) Alignment.End else Alignment.Start)
                ) {
                    Text(
                        text = reactionEmoji,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 13.sp
                    )
                }
            }

            // Retry prompt if message failed to send
            if (isFromMe && message.status == MessageStatus.FAILED) {
                OutlinedButton(
                    onClick = onRetryClick,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .testTag("retry_message_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Failed to send. Tap to retry", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
