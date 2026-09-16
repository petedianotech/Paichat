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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    val conversations by viewModel.conversations.collectAsState()
    val contactsMap by viewModel.contactsMap.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val drafts by viewModel.drafts.collectAsState()
    val matchedMessages by viewModel.matchedMessages.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (checkAllSmsPermissions(context)) {
            viewModel.syncSmsAndContacts(context)
        }
    }

    var selectedConversationForMenu by remember { mutableStateOf<ConversationEntity?>(null) }
    var conversationToDelete by remember { mutableStateOf<ConversationEntity?>(null) }
    var showQuickComposeDialog by remember { mutableStateOf(false) }
    var quickComposePhone by remember { mutableStateOf("") }
    var quickComposeText by remember { mutableStateOf("") }

    val pinnedConversations = conversations.filter { it.isPinned }
    val regularConversations = conversations.filter { !it.isPinned }
    val totalUnread = conversations.sumOf { it.unreadCount }
    val draftsCount = drafts.values.count { it.isNotBlank() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
            ) {
                // 1. Google Messages / Textra style integrated Search & Profile Bar
                Surface(
                    shape = RoundedCornerShape(26.dp),
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

                    if (draftsCount > 0) {
                        FilterChip(
                            selected = currentFilter == HomeFilter.DRAFTS,
                            onClick = { viewModel.setFilter(HomeFilter.DRAFTS) },
                            label = {
                                Text(
                                    text = "Drafts ($draftsCount)",
                                    fontSize = 13.sp,
                                    fontWeight = if (currentFilter == HomeFilter.DRAFTS) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                selectedContainerColor = MaterialTheme.colorScheme.error,
                                selectedLabelColor = MaterialTheme.colorScheme.onError
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
                        shape = RoundedCornerShape(20.dp),
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
            // Primary Start Chat Action
            ExtendedFloatingActionButton(
                onClick = onNavigateToNewChat,
                icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
                text = { Text("Start chat", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
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
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
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
                            text = when {
                                syncProgress.isSyncing -> "Loading conversations..."
                                searchQuery.isNotBlank() -> "No messages match your search"
                                else -> "No conversations yet"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when {
                                syncProgress.isSyncing -> "Retrieving your messages securely from device storage."
                                searchQuery.isNotBlank() -> "Try searching for a different name or number"
                                else -> "Tap 'Start chat' to send an SMS to any contact or phone number."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    if (searchQuery.isNotBlank()) {
                        // SEARCH MODE
                        if (conversations.isNotEmpty()) {
                            item {
                                Text(
                                    text = "CONVERSATIONS (${conversations.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
                                )
                            }

                            items(
                                items = conversations,
                                key = { "search_conv_${it.conversationId}" }
                            ) { conversation ->
                                val photoUri = contactsMap[conversation.phoneNumber]?.photoUri 
                                    ?: viewModel.getPhotoUriForPhone(conversation.phoneNumber)
                                ConversationItem(
                                    conversation = conversation,
                                    photoUri = photoUri,
                                    draftText = drafts[conversation.conversationId],
                                    onClick = { onNavigateToChat(conversation.conversationId) },
                                    onLongClick = { selectedConversationForMenu = conversation }
                                )
                            }
                        }

                        if (matchedMessages.isNotEmpty()) {
                            item {
                                Text(
                                    text = "MESSAGES (${matchedMessages.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)
                                )
                            }

                            items(
                                items = matchedMessages,
                                key = { "search_msg_${it.messageId}" }
                            ) { message ->
                                val contact = contactsMap[message.conversationId]
                                val displayName = contact?.name ?: message.senderPhoneNumber.ifBlank { message.recipientPhoneNumber }

                                Surface(
                                    onClick = { onNavigateToChat(message.conversationId) },
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = displayName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = com.example.ui.util.TimeFormatter.formatMessageTimestamp(message.timestamp),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        val content = message.content
                                        val isFromMe = message.senderPhoneNumber == "ME"
                                        val queryLower = searchQuery.lowercase()
                                        val textLower = content.lowercase()
                                        val annotated = buildAnnotatedString {
                                            if (isFromMe) {
                                                withStyle(
                                                    SpanStyle(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    append("You: ")
                                                }
                                            }
                                            var startIndex = 0
                                            while (startIndex < content.length) {
                                                val matchIndex = textLower.indexOf(queryLower, startIndex)
                                                if (matchIndex == -1) {
                                                    append(content.substring(startIndex))
                                                    break
                                                }
                                                if (matchIndex > startIndex) {
                                                    append(content.substring(startIndex, matchIndex))
                                                }
                                                withStyle(
                                                    SpanStyle(
                                                        background = MaterialTheme.colorScheme.primaryContainer,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                ) {
                                                    append(content.substring(matchIndex, matchIndex + queryLower.length))
                                                }
                                                startIndex = matchIndex + queryLower.length
                                            }
                                        }
                                        Text(
                                            text = annotated,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    } else {
                        // STANDARD CONVERSATION LIST WITH PINNED & SWIPE ACTIONS
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
                                val photoUri = contactsMap[conversation.phoneNumber]?.photoUri 
                                    ?: viewModel.getPhotoUriForPhone(conversation.phoneNumber)
                                SwipeableConversationItem(
                                    conversation = conversation,
                                    photoUri = photoUri,
                                    draftText = drafts[conversation.conversationId],
                                    onClick = { onNavigateToChat(conversation.conversationId) },
                                    onLongClick = { selectedConversationForMenu = conversation },
                                    onToggleRead = {
                                        viewModel.markAsReadOrUnread(conversation.conversationId, conversation.unreadCount)
                                    },
                                    onDelete = {
                                        viewModel.deleteConversationWithUndo(conversation.conversationId) { restoreAction ->
                                            coroutineScope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Conversation deleted",
                                                    actionLabel = "UNDO",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    restoreAction()
                                                }
                                            }
                                        }
                                    }
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
                            val photoUri = contactsMap[conversation.phoneNumber]?.photoUri 
                                ?: viewModel.getPhotoUriForPhone(conversation.phoneNumber)
                            SwipeableConversationItem(
                                conversation = conversation,
                                photoUri = photoUri,
                                draftText = drafts[conversation.conversationId],
                                onClick = { onNavigateToChat(conversation.conversationId) },
                                onLongClick = { selectedConversationForMenu = conversation },
                                onToggleRead = {
                                    viewModel.markAsReadOrUnread(conversation.conversationId, conversation.unreadCount)
                                },
                                onDelete = {
                                    viewModel.deleteConversationWithUndo(conversation.conversationId) { restoreAction ->
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Conversation deleted",
                                                actionLabel = "UNDO",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                restoreAction()
                                            }
                                        }
                                    }
                                }
                            )
                        }
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
                        shape = RoundedCornerShape(20.dp)
                    )
                    OutlinedTextField(
                        value = quickComposeText,
                        onValueChange = { quickComposeText = it },
                        label = { Text("Message") },
                        placeholder = { Text("Type SMS text...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(20.dp)
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
                    shape = CircleShape
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
