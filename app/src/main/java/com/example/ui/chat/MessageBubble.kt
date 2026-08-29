package com.example.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.data.local.entity.MessageType
import com.example.ui.theme.SmsBadgeBgDark
import com.example.ui.theme.SmsBadgeBgLight
import com.example.ui.theme.SmsBadgeTextDark
import com.example.ui.theme.SmsBadgeTextLight
import com.example.ui.theme.SmsBubbleDark
import com.example.ui.theme.SmsBubbleLight
import com.example.ui.util.TimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    isFromMe: Boolean,
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    reactionEmoji: String?,
    onLongClick: () -> Unit,
    onFallbackClick: () -> Unit
) {
    val outerRadius = 20.dp
    val innerRadius = 4.dp

    // Asymmetric Corner Radii calculation
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

    val backgroundColor = when {
        isFromMe -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    val contentColor = when {
        isFromMe -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
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
                .widthIn(max = 280.dp)
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

                // Text Content
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )

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
                                    contentDescription = "Sent",
                                    modifier = Modifier.size(12.dp),
                                    tint = contentColor
                                )
                            }
                            MessageStatus.DELIVERED -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Delivered",
                                    modifier = Modifier.size(14.dp),
                                    tint = contentColor
                                )
                            }
                            MessageStatus.READ -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Read",
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

        // Smart Fallback prompt if internet message failed
        if (isFromMe && message.status == MessageStatus.FAILED) {
            OutlinedButton(
                onClick = onFallbackClick,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .testTag("send_as_sms_fallback_button"),
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
                Text("Not delivered. Tap to send as SMS", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
