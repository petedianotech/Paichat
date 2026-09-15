package com.example.ui.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ConversationEntity
import com.example.ui.chat.ActionRowItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToChat: (conversationId: String) -> Unit,
    onNavigateToNewChat: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val conversations by viewModel.conversations.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()

    var selectedConversationForMenu by remember { mutableStateOf<ConversationEntity?>(null) }
    var conversationToDelete by remember { mutableStateOf<ConversationEntity?>(null) }
    var showQuickComposeDialog by remember { mutableStateOf(false) }
    var quickComposePhone by remember { mutableStateOf("") }
    var quickComposeText by remember { mutableStateOf("") }

    val pinnedConversations = conversations.filter { it.isPinned }
    val regularConversations = conversations.filter { !it.isPinned }
    val totalUnread = conversations.sumOf { it.unreadCount }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
            ) {
                // 1. Google Messages / Textra style integrated Search & Profile Bar
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("home_search_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search messages...",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.updateSearchQuery("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Quick Compose Bolt Action
                        IconButton(
                            onClick = { showQuickComposeDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Quick Compose",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Profile & Settings
                        IconButton(
                            onClick = onNavigateToProfile,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("home_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // 2. Filter Pills: All, Unread, Pinned
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = currentFilter == HomeFilter.ALL,
                        onClick = { viewModel.setFilter(HomeFilter.ALL) },
                        label = {
                            Text(
                                text = "All (${conversations.size})",
                                fontSize = 13.sp,
                                fontWeight = if (currentFilter == HomeFilter.ALL) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    FilterChip(
                        selected = currentFilter == HomeFilter.UNREAD,
                        onClick = { viewModel.setFilter(HomeFilter.UNREAD) },
                        label = {
                            Text(
                                text = if (totalUnread > 0) "Unread ($totalUnread)" else "Unread",
                                fontSize = 13.sp,
                                fontWeight = if (currentFilter == HomeFilter.UNREAD) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    if (pinnedConversations.isNotEmpty()) {
                        FilterChip(
                            selected = currentFilter == HomeFilter.PINNED,
                            onClick = { viewModel.setFilter(HomeFilter.PINNED) },
                            label = {
                                Text(
                                    text = "Pinned (${pinnedConversations.size})",
                                    fontSize = 13.sp,
                                    fontWeight = if (currentFilter == HomeFilter.PINNED) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                // 3. SMS Syncing / Importing Progress Indicator
                if (syncProgress.isSyncing) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = syncProgress.statusText.ifEmpty { "Syncing messages..." },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (syncProgress.total > 0) {
                                    Text(
                                        text = "${(syncProgress.progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (syncProgress.total > 0) {
                                LinearProgressIndicator(
                                    progress = { syncProgress.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Floating Quick Reply Launcher Shortcut
                if (conversations.isNotEmpty()) {
                    val latestConv = conversations.first()
                    FloatingActionButton(
                        onClick = {
                            val popupIntent = Intent(context, com.example.ui.quickreply.QuickReplyActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra("conversationId", latestConv.conversationId)
                                putExtra("senderPhone", latestConv.phoneNumber)
                                putExtra("senderName", latestConv.contactName)
                                putExtra("initialMessage", latestConv.lastMessage)
                            }
                            context.startActivity(popupIntent)
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("floating_quick_reply_bubble_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Floating Quick Reply",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Primary Start Chat Action
                ExtendedFloatingActionButton(
                    onClick = onNavigateToNewChat,
                    icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text("Start chat", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("start_chat_fab")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // SMS Permission and Default SMS Handler banner
            SmsPermissionBanner(
                onPermissionsGranted = {
                    viewModel.syncSmsAndContacts(context)
                }
            )

            if (conversations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No messages match your search" else "No conversations yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try searching for a different name or number" else "Tap 'Start chat' to send an SMS to any contact or phone number.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    if (pinnedConversations.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PINNED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        items(
                            items = pinnedConversations,
                            key = { it.conversationId }
                        ) { conversation ->
                            ConversationItem(
                                conversation = conversation,
                                onClick = { onNavigateToChat(conversation.conversationId) },
                                onLongClick = { selectedConversationForMenu = conversation }
                            )
                        }

                        if (regularConversations.isNotEmpty()) {
                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    items(
                        items = regularConversations,
                        key = { it.conversationId }
                    ) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            onClick = { onNavigateToChat(conversation.conversationId) },
                            onLongClick = { selectedConversationForMenu = conversation }
                        )
                    }
                }
            }
        }
    }

    // Quick Compose Dialog (Textra-style popup)
    if (showQuickComposeDialog) {
        AlertDialog(
            onDismissRequest = { showQuickComposeDialog = false },
            title = { Text("Quick Compose SMS", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = quickComposePhone,
                        onValueChange = { quickComposePhone = it },
                        label = { Text("To (Phone Number)") },
                        placeholder = { Text("e.g. +1 555-0199") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = quickComposeText,
                        onValueChange = { quickComposeText = it },
                        label = { Text("Message") },
                        placeholder = { Text("Type SMS text...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (quickComposePhone.isNotBlank() && quickComposeText.isNotBlank()) {
                            viewModel.quickCompose(quickComposePhone, quickComposeText) {
                                Toast.makeText(context, "SMS sent to $quickComposePhone", Toast.LENGTH_SHORT).show()
                            }
                            showQuickComposeDialog = false
                            quickComposePhone = ""
                            quickComposeText = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send SMS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickComposeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Long Press Conversation Menu Sheet
    selectedConversationForMenu?.let { conv ->
        ModalBottomSheet(
            onDismissRequest = { selectedConversationForMenu = null },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp)
            ) {
                Text(
                    text = conv.contactName ?: conv.phoneNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Pin / Unpin
                ActionRowItem(
                    icon = Icons.Default.PushPin,
                    label = if (conv.isPinned) "Unpin Conversation" else "Pin to Top",
                    onClick = {
                        viewModel.togglePin(conv.conversationId, conv.isPinned)
                        selectedConversationForMenu = null
                    }
                )

                // Mark as Read / Unread
                ActionRowItem(
                    icon = if (conv.unreadCount > 0) Icons.Default.MarkEmailRead else Icons.Default.MarkEmailUnread,
                    label = if (conv.unreadCount > 0) "Mark as Read" else "Mark as Unread",
                    onClick = {
                        viewModel.markAsReadOrUnread(conv.conversationId, conv.unreadCount)
                        selectedConversationForMenu = null
                    }
                )

                // Call
                ActionRowItem(
                    icon = Icons.Default.Call,
                    label = "Call Number",
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${conv.phoneNumber}"))
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                        }
                        selectedConversationForMenu = null
                    }
                )

                // Block
                ActionRowItem(
                    icon = Icons.Default.Block,
                    label = "Block Contact",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        viewModel.blockContact(conv.phoneNumber, conv.contactName)
                        selectedConversationForMenu = null
                        Toast.makeText(context, "Contact blocked", Toast.LENGTH_SHORT).show()
                    }
                )

                // Delete
                ActionRowItem(
                    icon = Icons.Default.Delete,
                    label = "Delete Conversation",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        val target = conv
                        selectedConversationForMenu = null
                        conversationToDelete = target
                    }
                )
            }
        }
    }

    // Delete Conversation Confirmation Dialog
    conversationToDelete?.let { conv ->
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("Delete conversation?") },
            text = { Text("Delete all messages with ${conv.contactName ?: conv.phoneNumber}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConversation(conv.conversationId)
                        conversationToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
