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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.data.local.entity.ScheduledMessageEntity
import com.example.ui.theme.LocalThemeGradient
import com.example.ui.util.AvatarUtil
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
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    val recordingDurationSec by viewModel.recordingDurationSec.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredMessages by viewModel.filteredMessages.collectAsState()

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
    val emojiList = listOf("❤️", "👍", "😂", "😮", "😢", "🔥")

    // Zero-permission Android Photo Picker for MMS
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.sendMediaAttachment(context, uri.toString())
        }
    }

    // Audio recording permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceRecording(context)
        } else {
            Toast.makeText(context, "Microphone permission is required for voice notes", Toast.LENGTH_SHORT).show()
        }
    }

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

    val imeBottom = WindowInsets.ime.getBottom(androidx.compose.ui.platform.LocalDensity.current)
    LaunchedEffect(imeBottom) {
        if (imeBottom > 0 && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
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
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = AvatarUtil.getInitials(contactDisplayName),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

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
                                }
                                Text(
                                    text = conversation?.phoneNumber ?: "",
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
                        // In-Thread Deep Search Action
                        IconButton(onClick = { viewModel.toggleSearch() }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (isSearchActive) "Close Search" else "Search Messages in Thread"
                            )
                        }

                        // Quick Call Action
                        val phone = conversation?.phoneNumber
                        if (!phone.isNullOrBlank()) {
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

                        // Options Menu
                        IconButton(onClick = { showTopMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }

                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            // Call Contact
                            DropdownMenuItem(
                                text = { Text("Call Contact") },
                                onClick = {
                                    showTopMenu = false
                                    if (!phone.isNullOrBlank()) {
                                        try {
                                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                            context.startActivity(dialIntent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) }
                            )

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
                            DropdownMenuItem(
                                text = { Text("Save Contact to Phone") },
                                onClick = {
                                    showTopMenu = false
                                    if (!phone.isNullOrBlank()) {
                                        saveContactToPhone(context, phone, conversation?.contactName)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.PersonAdd, contentDescription = null) }
                            )

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
                                    .clickable { viewModel.cycleNextSim() }
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
                                        text = "${currentSim?.displayName ?: "SIM 1"} (Tap to switch)",
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

                    // Input composer Row
                    if (isRecordingVoice) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Recording: ${String.format("%02d:%02d", recordingDurationSec / 60, recordingDurationSec % 60)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }

                                    IconButton(onClick = { viewModel.cancelVoiceRecording() }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Cancel Recording",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            FloatingActionButton(
                                onClick = {
                                    viewModel.stopAndSendVoiceRecording(context)
                                },
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("send_voice_note_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Voice Note",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { showAttachmentSheet = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Options & Attachments",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(onClick = { showQuickResponseSheet = true }) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = "Quick Responses",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    BasicTextField(
                                        value = inputText,
                                        onValueChange = { viewModel.updateInputText(it) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 6.dp)
                                            .testTag("chat_input_field"),
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface
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

                                    IconButton(onClick = { viewModel.updateInputText(inputText + "😊") }) {
                                        Icon(
                                            imageVector = Icons.Default.SentimentSatisfiedAlt,
                                            contentDescription = "Emoji",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            val isTyping = inputText.trim().isNotEmpty()
                            FloatingActionButton(
                                onClick = {
                                    if (isTyping) {
                                        executeWithSmsPermission {
                                            viewModel.initiateSendMessage(context)
                                        }
                                    } else {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                            viewModel.startVoiceRecording(context)
                                        } else {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                shape = CircleShape,
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .size(52.dp)
                                    .testTag("send_sms_button")
                            ) {
                                Icon(
                                    imageVector = if (isTyping) Icons.AutoMirrored.Filled.Send else Icons.Default.Mic,
                                    contentDescription = if (isTyping) "Send SMS" else "Record Voice Note",
                                    modifier = Modifier.size(24.dp)
                                )
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
                            onLongClick = { selectedMessageForAction = message },
                            onRetryClick = {
                                executeWithSmsPermission {
                                    viewModel.retryMessage(message.messageId)
                                }
                            }
                        )
                    }
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
                                        viewModel.deleteMessage(msg.messageId)
                                        selectedMessageForAction = null
                                    }
                                )
                            }
                        }
                    }
                }

                // Message Details Dialog
                showMessageInfoDialog?.let { msg ->
                    val len = msg.content.length
                    val segments = if (len == 0) 1 else ((len - 1) / 153) + 1
                    AlertDialog(
                        onDismissRequest = { showMessageInfoDialog = null },
                        title = { Text("Message Details", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Type: ${msg.messageType.name}")
                                Text("Status: ${msg.status.name}")
                                Text("Sent Time: ${TimeFormatter.formatFullDateTime(msg.timestamp)}")
                                Text("Characters: $len")
                                Text("SMS Segments: $segments")
                                Text("Sender: ${msg.senderPhoneNumber}")
                                Text("Recipient: ${msg.recipientPhoneNumber}")
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showMessageInfoDialog = null }) {
                                Text("Close")
                            }
                        }
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

                // Attachment & Options Bottom Sheet
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
                                text = "Attach & Schedule",
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
                                    icon = Icons.Default.Image,
                                    label = "Photo Gallery",
                                    onClick = {
                                        showAttachmentSheet = false
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )

                                AttachmentGridItem(
                                    icon = Icons.Default.Mic,
                                    label = "Voice Note",
                                    onClick = {
                                        showAttachmentSheet = false
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                            viewModel.startVoiceRecording(context)
                                        } else {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                )

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
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
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
