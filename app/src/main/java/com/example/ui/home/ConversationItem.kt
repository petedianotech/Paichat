package com.example.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ConversationEntity
import com.example.ui.components.ContactAvatar
import com.example.ui.util.AvatarUtil
import com.example.ui.util.TimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationItem(
    conversation: ConversationEntity,
    displayName: String? = null,
    photoUri: String? = null,
    draftText: String? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val contactName = displayName?.takeIf { it.isNotBlank() }
        ?: conversation.contactName?.takeIf { it.isNotBlank() }
        ?: conversation.phoneNumber
    val customColor = conversation.customColorHex?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { null }
    }
    val isUnread = conversation.unreadCount > 0
    val hasDraft = !draftText.isNullOrBlank()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("conversation_item_${conversation.conversationId}"),
        color = if (conversation.isPinned) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        } else {
            Color.Transparent
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Sleek Real Contact Avatar
                ContactAvatar(
                    name = contactName,
                    phoneNumber = conversation.phoneNumber,
                    photoUri = photoUri,
                    customColor = customColor,
                    size = 50.dp,
                    fontSize = 17.sp
                )

                Spacer(modifier = Modifier.width(14.dp))

                // 2. Middle Content: Name, Snippet, Time & Badges
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = contactName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (conversation.isPinned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Timestamp with crisp typography
                        Text(
                            text = TimeFormatter.formatMessageTimestamp(conversation.lastMessageTimestamp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 12.sp
                            ),
                            color = if (isUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (hasDraft) {
                            val draftAnnotated = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append("Draft: ")
                                }
                                withStyle(
                                    SpanStyle(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    append(draftText ?: "")
                                }
                            }
                            Text(
                                text = draftAnnotated,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Text(
                                text = conversation.lastMessage.ifBlank { "No messages" },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp
                                ),
                                color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Unread Count Capsule Badge or Dot
                        if (isUnread) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Inset Divider (iOS style separator aligned with text start)
            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                thickness = 0.5.dp
            )
        }
    }
}
