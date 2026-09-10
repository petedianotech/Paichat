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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
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
import com.example.ui.theme.LocalThemeGradient

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

    var isSearchActive by remember { mutableStateOf(false) }
    var selectedConversationForMenu by remember { mutableStateOf<ConversationEntity?>(null) }
    var conversationToDelete by remember { mutableStateOf<ConversationEntity?>(null) }
    var showQuickComposeDialog by remember { mutableStateOf(false) }
    var quickComposePhone by remember { mutableStateOf("") }
    var quickComposeText by remember { mutableStateOf("") }

    val backgroundGradient = LocalThemeGradient.current

    val pinnedConversations = conversations.filter { it.isPinned }
    val regularConversations = conversations.filter { !it.isPinned }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onSearch = { isSearchActive = false },
                        active = false,
                        onActiveChange = { isSearchActive = it },
                        placeholder = { Text("Search messages and contacts") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showQuickComposeDialog = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Quick Compose",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = onNavigateToProfile,
                                    modifier = Modifier.testTag("home_settings_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = SearchBarDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_bar")
                    ) {}

                    // Filter chips: All, Unread, Pinned
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentFilter == HomeFilter.ALL,
                            onClick = { viewModel.setFilter(HomeFilter.ALL) },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        FilterChip(
                            selected = currentFilter == HomeFilter.UNREAD,
                            onClick = { viewModel.setFilter(HomeFilter.UNREAD) },
                            label = { Text("Unread") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        FilterChip(
                            selected = currentFilter == HomeFilter.PINNED,
                            onClick = { viewModel.setFilter(HomeFilter.PINNED) },
                            label = { Text("Pinned") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToNewChat,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Start chat", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("start_chat_fab")
                )
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
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No messages match your search" else "No conversations yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
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
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PINNED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 1.sp
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
                                    Text(
                                        text = "MESSAGES",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
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
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = quickComposeText,
                            onValueChange = { quickComposeText = it },
                            label = { Text("Message") },
                            placeholder = { Text("Type SMS text...") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4,
                            shape = RoundedCornerShape(10.dp)
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
                        }
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
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
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
}
