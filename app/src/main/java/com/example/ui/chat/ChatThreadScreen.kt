package com.example.ui.chat

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.RadioButton
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Deselect
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.data.local.entity.ScheduledMessageEntity
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.LocalThemeGradient
import com.example.ui.util.AvatarUtil
import com.example.ui.util.PhoneNumberUtil
import com.example.ui.util.TimeFormatter
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val conversation by viewModel.conversation.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val scheduledMessages by viewModel.scheduledMessages.collectAsState()
    val quickResponses by viewModel.quickResponses.collectAsState()
    val availableSims by viewModel.availableSims.collectAsState()
    val selectedSimIndex by viewModel.selectedSimIndex.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val reactions by viewModel.reactions.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val delayedSendState by viewModel.delayedSendState.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredMessages by viewModel.filteredMessages.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()

    var showSimPickerDialog by remember { mutableStateOf(false) }

    var selectedMessageIds by remember { mutableStateOf(setOf<String>()) }
    val isSelectionMode = selectedMessageIds.isNotEmpty()
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }

    var selectedMessageForAction by remember { mutableStateOf<MessageEntity?>(null) }
    var showMessageInfoDialog by remember { mutableStateOf<MessageEntity?>(null) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showQuickResponseSheet by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showContactDetailsSheet by remember { mutableStateOf(false) }
    var showWallpaperPickerSheet by remember { mutableStateOf(false) }
    var newQuickResponseText by remember { mutableStateOf("") }
    var showAddQuickResponseDialog by remember { mutableStateOf(false) }
    var pendingActionAfterPermission by remember { mutableStateOf<(() -> Unit)?>(null) }

    val listState = rememberLazyListState()

    // Track active conversation to suppress redundant popups while user is in this chat
    val currentConvId = conversation?.conversationId
    androidx.compose.runtime.DisposableEffect(currentConvId) {
        if (!currentConvId.isNullOrBlank()) {
            com.example.ui.util.ActiveConversationTracker.activeConversationId = currentConvId
        }
        onDispose {
            if (com.example.ui.util.ActiveConversationTracker.activeConversationId == currentConvId) {
                com.example.ui.util.ActiveConversationTracker.activeConversationId = null
            }
        }
    }

    // Paging and Scroll Position Preservation State
    var isInitialLoad by remember { mutableStateOf(true) }
    var firstVisibleItemKey by remember { mutableStateOf<Any?>(null) }
    var firstVisibleItemOffset by remember { mutableStateOf(0) }

    val emojiList = listOf("❤️", "👍", "😂", "😮", "😢", "🔥")

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingActionAfterPermission?.invoke()
            pendingActionAfterPermission = null
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("SMS permission is required to send text messages.")
            }
        }
    }

    fun executeWithSmsPermission(action: () -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            action()
        } else {
            pendingActionAfterPermission = action
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSims(context)
    }

    // Connect snapshotFlow to track scroll position and detect near-top for loading older messages
    LaunchedEffect(listState) {
        // Collect first visible item info for scroll preservation
        launch {
            snapshotFlow {
                val layoutInfo = listState.layoutInfo
                val firstVisibleItem = layoutInfo.visibleItemsInfo.firstOrNull()
                Pair(firstVisibleItem?.key, firstVisibleItem?.offset ?: 0)
            }.collectLatest { (key, offset) ->
                firstVisibleItemKey = key
                firstVisibleItemOffset = offset
            }
        }
        
        // Detect near-top scroll to trigger pagination
        launch {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collectLatest { firstVisibleIndex ->
                    if (firstVisibleIndex <= 5 && messages.isNotEmpty() && !isInitialLoad) {
                        viewModel.loadMoreMessages()
                    }
                }
        }
    }

    val imeBottom = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0 && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isAtBottom = remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                true
            } else {
                val lastVisibleItem = visibleItems.last()
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 2
            }
        }
    }

    val visibleDateText by remember(listState, filteredMessages) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItem = layoutInfo.visibleItemsInfo.firstOrNull { it.key is String && (it.key as String).isNotEmpty() }
            if (visibleItem != null) {
                val key = visibleItem.key as? String
                val msg = filteredMessages.firstOrNull { it.messageId == key }
                msg?.timestamp?.let { com.example.ui.util.TimeFormatter.formatDateHeader(it) }
            } else null
        }
    }

    var lastMessageCount by remember { mutableStateOf(0) }
    var lastMessageId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            val newLastMessage = messages.last()
            val isNewMessageAdded = lastMessageId != null && newLastMessage.messageId != lastMessageId
            val isFromMe = newLastMessage.senderPhoneNumber == "ME"

            if (isInitialLoad) {
                listState.scrollToItem(messages.size - 1)
                isInitialLoad = false
            } else if (isNewMessageAdded && (isFromMe || isAtBottom.value)) {
                listState.animateScrollToItem(messages.size - 1)
            } else if (messages.size > lastMessageCount) {
                // Restore scroll position when older messages are prepended
                val oldKey = firstVisibleItemKey
                if (oldKey != null) {
                    val index = messages.indexOfFirst { it.messageId == oldKey }
                    if (index != -1) {
                        listState.scrollToItem(index, firstVisibleItemOffset)
                    }
                }
            }
            lastMessageId = newLastMessage.messageId
            lastMessageCount = messages.size
        }
    }

    val backgroundGradient = LocalThemeGradient.current
    val activeWallpaperId = conversation?.customWallpaper ?: appSettings.defaultChatWallpaper
    val activeWallpaper = ChatWallpapers.getWallpaperById(activeWallpaperId)

    val contactDisplayName = conversation?.contactName ?: conversation?.phoneNumber ?: "Chat"
    val avatarColor = AvatarUtil.getAvatarColor(conversation?.phoneNumber ?: "")

    // SMS Segment Calculator
    val textLen = inputText.length
    val maxCharsPerSms = if (textLen <= 160) 160 else 153
    val partCount = if (textLen == 0) 1 else ((textLen - 1) / 153) + 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
    ) {
        if (activeWallpaper != null) {
            Image(
                painter = painterResource(id = activeWallpaper.drawableRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (activeWallpaper.isDarkTheme) Color.Black.copy(alpha = 0.45f)
                        else Color.White.copy(alpha = 0.4f)
                    )
            )
        }

        Scaffold(
            modifier = Modifier.imePadding(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (isSelectionMode) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "${selectedMessageIds.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { selectedMessageIds = emptySet() }) {
                                Icon(Icons.Default.Close, contentDescription = "Exit Selection Mode")
                            }
                        },
                        actions = {
                            // Select All / Deselect All
                            val allSelected = filteredMessages.isNotEmpty() && filteredMessages.all { it.messageId in selectedMessageIds }
                            IconButton(onClick = {
                                selectedMessageIds = if (allSelected) {
                                    emptySet()
                                } else {
                                    filteredMessages.map { it.messageId }.toSet()
                                }
                            }) {
                                Icon(
                                    imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                    contentDescription = if (allSelected) "Deselect All" else "Select All"
                                )
                            }

                            // Details (if single message selected)
                            if (selectedMessageIds.size == 1) {
                                val singleMsgId = selectedMessageIds.first()
                                val singleMsg = filteredMessages.firstOrNull { it.messageId == singleMsgId }
                                if (singleMsg != null) {
                                    IconButton(onClick = {
                                        showMessageInfoDialog = singleMsg
                                    }) {
                                        Icon(Icons.Default.Info, contentDescription = "Message Details")
                                    }
                                }
                            }

                            // Copy Selected Messages
                            IconButton(onClick = {
                                val selectedMsgs = filteredMessages.filter { it.messageId in selectedMessageIds }
                                    .sortedBy { it.timestamp }
                                val combinedText = selectedMsgs.joinToString(separator = "\n\n") { it.content }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Copied Messages", combinedText))
                                Toast.makeText(context, "${selectedMsgs.size} message(s) copied", Toast.LENGTH_SHORT).show()
                                selectedMessageIds = emptySet()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Selected")
                            }

                            // Share Selected Messages
                            IconButton(onClick = {
                                val selectedMsgs = filteredMessages.filter { it.messageId in selectedMessageIds }
                                    .sortedBy { it.timestamp }
                                val combinedText = selectedMsgs.joinToString(separator = "\n\n") { it.content }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, combinedText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Messages via"))
                                selectedMessageIds = emptySet()
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share Selected")
                            }

                            // Delete Selected Messages
                            IconButton(onClick = {
                                showDeleteConfirmationDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Selected",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                } else {
                    val phone = conversation?.phoneNumber
                    val name = conversation?.contactName
                    val contactType: PhoneNumberUtil.ContactType = remember(phone, name) {
                        PhoneNumberUtil.getContactType(phone, name)
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        TopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showContactDetailsSheet = true }
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                ) {
                                    ContactAvatar(
                                        photoUri = viewModel.getContactPhotoUri(),
                                        name = contactDisplayName,
                                        phoneNumber = conversation?.phoneNumber ?: "",
                                        size = 40.dp
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = contactDisplayName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (conversation?.isPinned == true) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.PushPin,
                                                    contentDescription = "Pinned",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            if (isMuted) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.NotificationsOff,
                                                    contentDescription = "Muted",
                                                    modifier = Modifier.size(14.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        val subtitleText = when (contactType) {
                                            PhoneNumberUtil.ContactType.SERVICE_MESSAGE -> "Automated Service • Verified SMS"
                                            PhoneNumberUtil.ContactType.UNKNOWN_NUMBER -> conversation?.phoneNumber ?: "Unknown"
                                            PhoneNumberUtil.ContactType.SAVED_CONTACT -> conversation?.phoneNumber ?: ""
                                        }
                                        Text(
                                            text = subtitleText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            actions = {
                                // 1. Quick Call Action (hidden for automated service senders)
                                if (!phone.isNullOrBlank() && contactType != PhoneNumberUtil.ContactType.SERVICE_MESSAGE) {
                                    IconButton(onClick = {
                                        try {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                            context.startActivity(dialIntent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(Icons.Default.Call, contentDescription = "Call Contact")
                                    }
                                }

                                // 2. In-Thread Deep Search Action
                                IconButton(onClick = { viewModel.toggleSearch() }) {
                                    Icon(
                                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = if (isSearchActive) "Close Search" else "Search Messages in Thread"
                                    )
                                }

                                // 3. Options Menu
                                IconButton(onClick = { showTopMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                                }

                                DropdownMenu(
                                    expanded = showTopMenu,
                                    onDismissRequest = { showTopMenu = false }
                                ) {
                                    // Call Contact (only if callable)
                                    if (!phone.isNullOrBlank() && contactType != PhoneNumberUtil.ContactType.SERVICE_MESSAGE) {
                                        DropdownMenuItem(
                                            text = { Text("Call Contact") },
                                            onClick = {
                                                showTopMenu = false
                                                try {
                                                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                                    context.startActivity(dialIntent)
                                                } catch (_: Exception) {
                                                    Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) }
                                        )
                                    }

                                    // Mute / Unmute Notifications
                                    DropdownMenuItem(
                                        text = { Text(if (isMuted) "Unmute Notifications" else "Mute Notifications") },
                                        onClick = {
                                            showTopMenu = false
                                            viewModel.toggleMute()
                                            val msg = if (!isMuted) "Notifications muted for this chat" else "Notifications unmuted"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (isMuted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                                contentDescription = null
                                            )
                                        }
                                    )

                                    // Dual SIM preference selector
                                    if (availableSims.size > 1) {
                                        DropdownMenuItem(
                                            text = { Text("Select SIM for Chat") },
                                            onClick = {
                                                showTopMenu = false
                                                showSimPickerDialog = true
                                            },
                                            leadingIcon = { Icon(Icons.Default.SimCard, contentDescription = null) }
                                        )
                                    }

                                    // View Contact Details
                                    DropdownMenuItem(
                                        text = { Text("View Contact Details") },
                                        onClick = {
                                            showTopMenu = false
                                            showContactDetailsSheet = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.ContactPage, contentDescription = null) }
                                    )

                                    // Save Contact to Phone
                                    if (contactType == PhoneNumberUtil.ContactType.UNKNOWN_NUMBER && !phone.isNullOrBlank()) {
                                        DropdownMenuItem(
                                            text = { Text("Save Contact to Phone") },
                                            onClick = {
                                                showTopMenu = false
                                                saveContactToPhone(context, phone, conversation?.contactName)
                                            },
                                            leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) }
                                        )
                                    }

                                    // Choose Chat Wallpaper
                                    DropdownMenuItem(
                                        text = { Text("Chat Wallpaper") },
                                        onClick = {
                                            showTopMenu = false
                                            showWallpaperPickerSheet = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) }
                                    )

                                    // Conversation Color
                                    DropdownMenuItem(
                                        text = { Text("Conversation Color") },
                                        onClick = {
                                            showColorPicker = true
                                            showTopMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.ColorLens, contentDescription = null) }
                                    )

                                    // Pin / Unpin
                                    DropdownMenuItem(
                                        text = { Text(if (conversation?.isPinned == true) "Unpin Conversation" else "Pin to Top") },
                                        onClick = {
                                            viewModel.togglePin()
                                            showTopMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                                    )

                                    // Block / Unblock Number
                                    DropdownMenuItem(
                                        text = { Text(if (conversation?.isBlocked == true) "Unblock Number" else "Block Number") },
                                        onClick = {
                                            showTopMenu = false
                                            if (conversation?.isBlocked == true) {
                                                viewModel.unblockContact()
                                            } else {
                                                viewModel.blockContact()
                                                onNavigateBack()
                                            }
                                        },
                                        leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) }
                                    )
                                }
                            }
                        )

                        // 4. Unknown Number Banner
                        if (contactType == PhoneNumberUtil.ContactType.UNKNOWN_NUMBER && !phone.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PersonOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Unsaved number",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    androidx.compose.material3.TextButton(
                                        onClick = { saveContactToPhone(context, phone, null) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Add to Contacts", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Delayed Send Indicator & Undo Banner
                    AnimatedVisibility(
                        visible = delayedSendState != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        delayedSendState?.let { state ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            progress = { state.remainingSeconds / state.totalSeconds.toFloat() },
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.5.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Sending in ${String.format("%.1f", state.remainingSeconds)}s...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Button(
                                            onClick = { viewModel.cancelDelayedSend() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("UNDO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        IconButton(
                                            onClick = { viewModel.sendDelayedNow(context) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Send immediately",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SMS Character and Segment Counter + Dual SIM indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dual SIM Selector Chip
                        val currentSim = availableSims.getOrNull(selectedSimIndex)
                        if (availableSims.size > 1) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clickable { showSimPickerDialog = true }
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SimCard,
                                        contentDescription = "Switch SIM",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "SIM ${selectedSimIndex + 1}: ${currentSim?.displayName ?: "SIM"} (Tap to change)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        if (appSettings.showCharacterCounter && inputText.isNotEmpty()) {
                            Text(
                                text = "$textLen/$maxCharsPerSms (${partCount} SMS)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Input composer Row (Pure SMS)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp, max = 130.dp)
                                .wrapContentHeight(),
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                IconButton(
                                    onClick = { showAttachmentSheet = true },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Options & Attachments",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showQuickResponseSheet = true },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Quick Responses",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                BasicTextField(
                                    value = inputText,
                                    onValueChange = { viewModel.updateInputText(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 6.dp, vertical = 10.dp)
                                        .testTag("chat_input_field"),
                                    maxLines = 5,
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 20.sp
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    decorationBox = { innerTextField ->
                                        if (inputText.isEmpty()) {
                                            Text(
                                                text = "Text message (SMS)...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val isTyping = inputText.trim().isNotEmpty()
                        Box {
                            FloatingActionButton(
                                onClick = {
                                    if (isTyping) {
                                        executeWithSmsPermission {
                                            viewModel.initiateSendMessage(context)
                                        }
                                    } else {
                                        Toast.makeText(context, "Type a message to send SMS", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = CircleShape,
                                containerColor = if (isTyping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("send_sms_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send SMS",
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (isTyping && availableSims.size > 1) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(16.dp)
                                        .clickable { showSimPickerDialog = true }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${selectedSimIndex + 1}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                ) {
                    // Deep Thread Search Input Banner
                    if (isSearchActive) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.setSearchQuery(it) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        decorationBox = { innerTextField ->
                                            if (searchQuery.isEmpty()) {
                                                Text(
                                                    text = "Search in thread...",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        Text(
                                            text = "${filteredMessages.size} match${if (filteredMessages.size != 1) "es" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                        IconButton(
                                            onClick = { viewModel.clearSearch() },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear search text",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pending Scheduled Messages Banner in Thread
                    if (scheduledMessages.isNotEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Scheduled Messages (${scheduledMessages.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                    scheduledMessages.forEach { scheduled ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "\"${scheduled.content}\"",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Text(
                                                    text = "Sending at: ${TimeFormatter.formatMessageTimestamp(scheduled.scheduledTimestamp)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                                )
                                            }

                                            Row {
                                                IconButton(
                                                    onClick = {
                                                        executeWithSmsPermission {
                                                            viewModel.sendScheduledMessageNow(scheduled)
                                                        }
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.AutoMirrored.Filled.Send,
                                                        contentDescription = "Send now",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { viewModel.cancelScheduledMessage(scheduled.id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Cancel scheduled",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Conversation Messages List
                    itemsIndexed(
                        items = filteredMessages,
                        key = { _, msg -> msg.messageId }
                    ) { index, message ->
                        val isFromMe = message.senderPhoneNumber == "ME"
                        val isFirstInGroup = index == 0 || filteredMessages[index - 1].senderPhoneNumber != message.senderPhoneNumber
                        val isLastInGroup = index == filteredMessages.size - 1 || filteredMessages[index + 1].senderPhoneNumber != message.senderPhoneNumber
                        val isSelected = selectedMessageIds.contains(message.messageId)

                        MessageBubble(
                            message = message,
                            isFromMe = isFromMe,
                            isFirstInGroup = isFirstInGroup,
                            isLastInGroup = isLastInGroup,
                            reactionEmoji = reactions[message.messageId],
                            bubbleShape = appSettings.bubbleShape,
                            fontSize = appSettings.fontSize,
                            showDeliveryMarks = appSettings.deliveryReportMode in listOf("BOTH", "MARKS_ONLY") || (appSettings.deliveryReportMode == "DEFAULT" && appSettings.deliveryReports),
                            customColorHex = conversation?.customColorHex,
                            highlightQuery = searchQuery,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onClick = {
                                if (isSelectionMode) {
                                    selectedMessageIds = if (isSelected) {
                                        selectedMessageIds - message.messageId
                                    } else {
                                        selectedMessageIds + message.messageId
                                    }
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    selectedMessageIds = setOf(message.messageId)
                                } else {
                                    selectedMessageIds = if (isSelected) {
                                        selectedMessageIds - message.messageId
                                    } else {
                                        selectedMessageIds + message.messageId
                                    }
                                }
                            },
                            onRetryClick = {
                                executeWithSmsPermission {
                                    viewModel.retryMessage(message.messageId)
                                }
                            }
                        )
                    }
                }

                // Floating Date Indicator on Scroll
                AnimatedVisibility(
                    visible = listState.isScrollInProgress && visibleDateText != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp)
                ) {
                    visibleDateText?.let { dateText ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
                            tonalElevation = 4.dp,
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Return to Latest (Scroll to bottom) Floating Action Button
                AnimatedVisibility(
                    visible = !isAtBottom.value && filteredMessages.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 12.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(filteredMessages.size - 1)
                            }
                        },
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Scroll to bottom",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Multi-select Delete Confirmation Dialog
                if (showDeleteConfirmationDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmationDialog = false },
                        title = { Text("Delete selected messages?", fontWeight = FontWeight.Bold) },
                        text = { Text("Are you sure you want to delete ${selectedMessageIds.size} message(s)?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val count = selectedMessageIds.size
                                    val targets = selectedMessageIds.toList()
                                    selectedMessageIds = emptySet()
                                    showDeleteConfirmationDialog = false

                                    viewModel.deleteMessagesWithUndo(targets) { restoreAction ->
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "$count message(s) deleted",
                                                actionLabel = "UNDO",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                restoreAction()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete", color = MaterialTheme.colorScheme.onError)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirmationDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Message Action Dialog
                selectedMessageForAction?.let { msg ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { selectedMessageForAction = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .widthIn(max = 320.dp)
                                .padding(24.dp)
                                .clickable(enabled = false) {}
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Reactions",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    emojiList.forEach { emoji ->
                                        Text(
                                            text = emoji,
                                            fontSize = 24.sp,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable {
                                                    viewModel.addReaction(msg.messageId, emoji)
                                                    selectedMessageForAction = null
                                                }
                                                .padding(6.dp)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                ActionRowItem(
                                    icon = Icons.Default.Check,
                                    label = "Select Message",
                                    onClick = {
                                        selectedMessageIds = setOf(msg.messageId)
                                        selectedMessageForAction = null
                                    }
                                )

                                ActionRowItem(
                                    icon = Icons.Default.ContentCopy,
                                    label = "Copy Entire Text",
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("SMS Message", msg.content)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        selectedMessageForAction = null
                                    }
                                )

                                ActionRowItem(
                                    icon = Icons.Default.Share,
                                    label = "Forward / Share Message",
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, msg.content)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share SMS via"))
                                        selectedMessageForAction = null
                                    }
                                )

                                ActionRowItem(
                                    icon = Icons.Default.Info,
                                    label = "Message Details",
                                    onClick = {
                                        val target = msg
                                        selectedMessageForAction = null
                                        showMessageInfoDialog = target
                                    }
                                )

                                ActionRowItem(
                                    icon = Icons.Default.Delete,
                                    label = "Delete Message",
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = {
                                        val targetId = msg.messageId
                                        selectedMessageForAction = null
                                        viewModel.deleteMessagesWithUndo(listOf(targetId)) { restoreAction ->
                                            coroutineScope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "Message deleted",
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

                // Message Details Dialog
                showMessageInfoDialog?.let { msg ->
                    MessageDetailsDialog(
                        message = msg,
                        contactName = conversation?.contactName,
                        onDismiss = { showMessageInfoDialog = null }
                    )
                }

                // Conversation Color Picker Dialog
                if (showColorPicker) {
                    val colors = listOf(
                        "Default Theme" to null,
                        "Blue Accent" to "#0288D1",
                        "Indigo Accent" to "#3F51B5",
                        "Emerald Accent" to "#2E7D32",
                        "Violet Accent" to "#7B1FA2",
                        "Rose Accent" to "#E91E63",
                        "Amber Accent" to "#FF8F00"
                    )
                    AlertDialog(
                        onDismissRequest = { showColorPicker = false },
                        title = { Text("Conversation Bubble Color", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                colors.forEach { (name, hex) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.setCustomColor(hex)
                                                showColorPicker = false
                                            }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (hex != null) Color(android.graphics.Color.parseColor(hex))
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = name, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.setCustomColor(null)
                                showColorPicker = false
                            }) {
                                Text("Reset to Default")
                            }
                        }
                    )
                }

                // Quick Response Bottom Sheet
                if (showQuickResponseSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showQuickResponseSheet = false },
                        sheetState = rememberModalBottomSheetState()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Quick Canned Responses",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { showAddQuickResponseDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add New")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            quickResponses.forEach { qr ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            viewModel.appendQuickResponse(qr.text)
                                            showQuickResponseSheet = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = qr.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (!qr.isDefault) {
                                            IconButton(
                                                onClick = { viewModel.deleteQuickResponse(qr.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // Add Quick Response Dialog
                if (showAddQuickResponseDialog) {
                    AlertDialog(
                        onDismissRequest = { showAddQuickResponseDialog = false },
                        title = { Text("New Quick Response", fontWeight = FontWeight.Bold) },
                        text = {
                            OutlinedTextField(
                                value = newQuickResponseText,
                                onValueChange = { newQuickResponseText = it },
                                placeholder = { Text("e.g. In a meeting, will call you later!") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                viewModel.addNewQuickResponse(newQuickResponseText)
                                newQuickResponseText = ""
                                showAddQuickResponseDialog = false
                            }) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddQuickResponseDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Schedule Message Dialog
                if (showScheduleDialog) {
                    ScheduleTimePickerDialog(
                        onDismiss = { showScheduleDialog = false },
                        onScheduleConfirmed = { timestamp ->
                            viewModel.scheduleMessage(timestamp)
                            showScheduleDialog = false
                            Toast.makeText(context, "Message scheduled successfully", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Contact Details Bottom Sheet
                if (showContactDetailsSheet) {
                    ContactDetailsSheet(
                        conversation = conversation,
                        totalMessagesCount = messages.size,
                        onDismiss = { showContactDetailsSheet = false },
                        onTogglePin = { viewModel.togglePin() },
                        onToggleBlock = {
                            if (conversation?.isBlocked == true) {
                                viewModel.unblockContact()
                            } else {
                                viewModel.blockContact()
                            }
                        },
                        onChangeColor = { showColorPicker = true },
                        onChangeWallpaper = { showWallpaperPickerSheet = true }
                    )
                }

                // Chat Wallpaper Picker Bottom Sheet
                if (showWallpaperPickerSheet) {
                    ChatWallpaperPickerSheet(
                        currentWallpaperId = activeWallpaperId,
                        onDismiss = { showWallpaperPickerSheet = false },
                        onSelectWallpaper = { wallpaperId ->
                            viewModel.setCustomWallpaper(wallpaperId)
                        }
                    )
                }

                // Actions & Attachments Bottom Sheet (SMS Tools)
                if (showAttachmentSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showAttachmentSheet = false },
                        sheetState = rememberModalBottomSheetState()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "SMS Tools & Actions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                AttachmentGridItem(
                                    icon = Icons.Default.Schedule,
                                    label = "Schedule SMS",
                                    onClick = {
                                        showAttachmentSheet = false
                                        showScheduleDialog = true
                                    }
                                )

                                AttachmentGridItem(
                                    icon = Icons.Default.Bolt,
                                    label = "Templates",
                                    onClick = {
                                        showAttachmentSheet = false
                                        showQuickResponseSheet = true
                                    }
                                )

                                AttachmentGridItem(
                                    icon = Icons.Default.LocationOn,
                                    label = "Location",
                                    onClick = {
                                        viewModel.updateInputText("📍 My Location: https://maps.google.com/?q=37.7749,-122.4194")
                                        showAttachmentSheet = false
                                    }
                                )

                                AttachmentGridItem(
                                    icon = Icons.Default.ContactPage,
                                    label = "Contact Card",
                                    onClick = {
                                        viewModel.updateInputText("👤 Contact: $contactDisplayName (${conversation?.phoneNumber})")
                                        showAttachmentSheet = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dual SIM Selection Dialog
        if (showSimPickerDialog && availableSims.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showSimPickerDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SimCard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select SIM for this Chat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Choose preferred SIM card for sending text messages in this conversation:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        availableSims.forEachIndexed { index, sim ->
                            val isSelected = index == selectedSimIndex
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        viewModel.setConversationSim(index)
                                        showSimPickerDialog = false
                                        Toast.makeText(context, "Set to SIM ${index + 1} (${sim.displayName})", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.setConversationSim(index)
                                            showSimPickerDialog = false
                                            Toast.makeText(context, "Set to SIM ${index + 1} (${sim.displayName})", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "SIM ${index + 1}: ${sim.displayName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        if (sim.carrierName.isNotBlank() && sim.carrierName != sim.displayName) {
                                            Text(
                                                text = sim.carrierName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showSimPickerDialog = false }) {
                        Text("Done")
                    }
                }
            )
        }
    }
}

@Composable
fun ScheduleTimePickerDialog(
    onDismiss: () -> Unit,
    onScheduleConfirmed: (Long) -> Unit
) {
    val context = LocalContext.current
    var selectedCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Message", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
                            onScheduleConfirmed(cal.timeInMillis)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("In 1 Hour", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 20)
                                set(Calendar.MINUTE, 0)
                                if (timeInMillis <= System.currentTimeMillis()) {
                                    add(Calendar.DAY_OF_YEAR, 1)
                                }
                            }
                            onScheduleConfirmed(cal.timeInMillis)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Tonight 8PM", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                            }
                            onScheduleConfirmed(cal.timeInMillis)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Tomorrow 9AM", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Custom Date & Time:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        val currentCal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        val pickedCal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                                            set(Calendar.MINUTE, minute)
                                            set(Calendar.SECOND, 0)
                                        }
                                        if (pickedCal.timeInMillis > System.currentTimeMillis()) {
                                            onScheduleConfirmed(pickedCal.timeInMillis)
                                        } else {
                                            Toast.makeText(context, "Scheduled time must be in the future", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    currentCal.get(Calendar.HOUR_OF_DAY),
                                    currentCal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            currentCal.get(Calendar.YEAR),
                            currentCal.get(Calendar.MONTH),
                            currentCal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick Custom Date & Time")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AttachmentGridItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ActionRowItem(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}
