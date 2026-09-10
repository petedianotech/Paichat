package com.example.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.ui.util.TimeFormatter

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
    customColorHex: String? = null,
    onLongClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    val outerRadius = when (bubbleShape) {
        "PILL" -> 24.dp
        "SQUARE" -> 8.dp
        else -> 18.dp
    }
    val innerRadius = when (bubbleShape) {
        "PILL" -> 10.dp
        "SQUARE" -> 4.dp
        else -> 4.dp
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
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (_: Exception) {
            null
        }
    }

    val backgroundColor = when {
        isFromMe -> customColor ?: MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
    }

    val contentColor = when {
        isFromMe -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val textStyle = when (fontSize) {
        "SMALL" -> MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
        "LARGE" -> MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp)
        "EXTRA_LARGE" -> MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp)
        else -> MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = shape,
            color = backgroundColor,
            contentColor = contentColor,
            modifier = Modifier
                .widthIn(max = 290.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
                .testTag("message_bubble_${message.messageId}")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Media preview if present
                if (!message.mediaUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "Attachment",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Text Content with Partial Selection Container
                SelectionContainer {
                    Text(
                        text = message.content,
                        style = textStyle,
                        color = contentColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom row: timestamp + status tick
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = TimeFormatter.formatMessageTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = contentColor.copy(alpha = 0.7f)
                    )

                    if (isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            MessageStatus.SENDING -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sending",
                                    modifier = Modifier.size(12.dp),
                                    tint = contentColor.copy(alpha = 0.6f)
                                )
                            }
                            MessageStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sent (1 mark)",
                                    modifier = Modifier.size(12.dp),
                                    tint = contentColor
                                )
                            }
                            MessageStatus.DELIVERED, MessageStatus.READ -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Delivered (2 marks)",
                                    modifier = Modifier.size(14.dp),
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
                        }
                    }
                }
            }
        }

        // Display Emoji Reaction if attached
        if (!reactionEmoji.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .align(if (isFromMe) Alignment.End else Alignment.Start)
            ) {
                Text(
                    text = reactionEmoji,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 14.sp
                )
            }
        }

        // Retry prompt if SMS failed to send
        if (isFromMe && message.status == MessageStatus.FAILED) {
            OutlinedButton(
                onClick = onRetryClick,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("retry_message_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Failed to send. Tap to retry", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
