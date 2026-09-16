package com.example.ui.quickreply

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.local.database.PulseChatDatabase
import com.example.data.local.entity.MessageEntity
import com.example.data.preference.UserPreferences
import com.example.data.repository.ContactRepository
import com.example.data.repository.MessageRepository
import com.example.ui.components.ContactAvatar
import com.example.ui.theme.PaiChatTheme
import com.example.ui.util.AvatarUtil
import com.example.ui.util.TimeFormatter
import kotlinx.coroutines.launch

/**
 * Textra-style Preview & Quick Reply Floating Overlay Activity.
 * Runs in an isolated task (taskAffinity="com.example.quickreply", excludeFromRecents=true).
 * When dismissed, it finishes immediately and returns to whatever app was underneath (YouTube, Browser, etc.)
 * without bringing the main messaging activity to the foreground.
 *
 * NOTE: Dismissing or tapping outside does NOT dismiss the system notification from the shade.
 * The unread notification persists until explicitly replied to, marked as read, or swiped away.
 */
class QuickReplyActivity : ComponentActivity() {

    private var activeConversationId by mutableStateOf("")
    private var activeSenderPhone by mutableStateOf("")
    private var activeSenderName by mutableStateOf<String?>(null)
    private var activeInitialMessage by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        updateFromIntent(intent)

        if (activeConversationId.isBlank()) {
            dismissAndClose()
            return
        }

        val userPreferences = UserPreferences(applicationContext)
        val database = PulseChatDatabase.getDatabase(applicationContext)
        val contactRepository = ContactRepository()
        val messageRepository = MessageRepository(
            context = applicationContext,
            conversationDao = database.conversationDao(),
            messageDao = database.messageDao(),
            scheduledMessageDao = database.scheduledMessageDao(),
            blockedContactDao = database.blockedContactDao(),
            quickResponseDao = database.quickResponseDao(),
            contactRepository = contactRepository,
            userPreferences = userPreferences
        )

        setContent {
            val appSettings by userPreferences.appSettings.collectAsState()

            PaiChatTheme(
                themeMode = appSettings.themeMode,
                colorTheme = appSettings.colorTheme,
                fontFamily = appSettings.fontFamily
            ) {
                QuickReplyPopupScreen(
                    conversationId = activeConversationId,
                    senderPhone = activeSenderPhone,
                    senderName = activeSenderName,
                    initialMessage = activeInitialMessage,
                    messageRepository = messageRepository,
                    contactRepository = contactRepository,
                    userPreferences = userPreferences,
                    onDismiss = { dismissAndClose() },
                    onMarkRead = {
                        val phone = activeSenderPhone
                        val convId = activeConversationId
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            messageRepository.clearUnreadCount(convId)
                            NotificationManagerCompat.from(applicationContext).cancel(phone.hashCode())
                        }
                        Toast.makeText(applicationContext, "Marked as read", Toast.LENGTH_SHORT).show()
                        dismissAndClose()
                    },
                    onOpenFullChat = {
                        val openIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("conversationId", activeConversationId)
                        }
                        startActivity(openIntent)
                        dismissAndClose()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateFromIntent(intent)
    }

    private fun updateFromIntent(intent: Intent?) {
        if (intent == null) return
        val convId = intent.getStringExtra("conversationId") ?: intent.getStringExtra("senderPhone") ?: ""
        val phone = intent.getStringExtra("senderPhone") ?: convId
        val name = intent.getStringExtra("senderName")
        val msg = intent.getStringExtra("initialMessage") ?: ""

        if (convId.isNotBlank()) {
            activeConversationId = convId
            activeSenderPhone = phone
            activeSenderName = name
            activeInitialMessage = msg
        }
    }

    private fun dismissAndClose() {
        finishAndRemoveTask()
        overridePendingTransition(0, 0)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        dismissAndClose()
    }
}

@Composable
fun QuickReplyPopupScreen(
    conversationId: String,
    senderPhone: String,
    senderName: String?,
    initialMessage: String,
    messageRepository: MessageRepository,
    contactRepository: ContactRepository,
    userPreferences: UserPreferences,
    onDismiss: () -> Unit,
    onMarkRead: () -> Unit,
    onOpenFullChat: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var replyText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var showSizeMenu by remember { mutableStateOf(false) }

    val contactPhotoUri = remember(senderPhone) {
        contactRepository.resolveContactPhotoUri(context, senderPhone)
    }

    val appSettings by userPreferences.appSettings.collectAsState()
    val messages by messageRepository.getMessagesForConversationPaged(conversationId, 25)
        .collectAsState(initial = emptyList())

    val quickResponses by messageRepository.getAllQuickResponses()
        .collectAsState(initial = emptyList())

    val listState = rememberLazyListState()

    // Dynamic Preview Sizing based on user preference
    val (widthFraction, heightFraction) = when (appSettings.popupPreviewSize) {
        "COMPACT" -> Pair(0.88f, 0.50f)
        "LARGE" -> Pair(0.96f, 0.84f)
        else -> Pair(0.92f, 0.68f) // STANDARD
    }

    // Check system overlay permission ("Appear on top of other apps")
    val canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }

    // Scroll to latest message when new messages arrive or keyboard opens
    val imeHeight = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(imeHeight) {
        if (imeHeight > 0 && messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val displayName = senderName ?: senderPhone
    val avatarBgColor = remember(displayName) { AvatarUtil.getAvatarColor(displayName) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .fillMaxHeight(heightFraction)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Intercept tap on card body so it doesn't dismiss */ }
                .testTag("quick_reply_popup_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // 1. Header Bar: Avatar, Contact info, Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        ContactAvatar(
                            photoUri = contactPhotoUri,
                            name = displayName,
                            phoneNumber = senderPhone,
                            size = 40.dp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Text(
                                text = senderPhone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Mark Read Action
                        IconButton(
                            onClick = onMarkRead,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Mark as read",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Size Selection Dropdown Button
                        Box {
                            IconButton(
                                onClick = { showSizeMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AspectRatio,
                                    contentDescription = "Change Preview Size",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showSizeMenu,
                                onDismissRequest = { showSizeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Compact Preview") },
                                    trailingIcon = if (appSettings.popupPreviewSize == "COMPACT") {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    onClick = {
                                        userPreferences.setPopupPreviewSize("COMPACT")
                                        showSizeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Standard Preview") },
                                    trailingIcon = if (appSettings.popupPreviewSize == "STANDARD") {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    onClick = {
                                        userPreferences.setPopupPreviewSize("STANDARD")
                                        showSizeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Large Preview") },
                                    trailingIcon = if (appSettings.popupPreviewSize == "LARGE") {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    onClick = {
                                        userPreferences.setPopupPreviewSize("LARGE")
                                        showSizeMenu = false
                                    }
                                )
                            }
                        }

                        // Open Full App Chat Action
                        IconButton(
                            onClick = onOpenFullChat,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open full chat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Dismiss/Close popup
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss popup",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Overlay Permission Banner (if not granted on Android M+)
                if (!canDrawOverlays && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Enable 'Appear on top' to reply over other apps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            TextButton(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }
                            ) {
                                Text("Grant", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                )
                Spacer(modifier = Modifier.height(8.dp))

                // 2. Scrollable Message Thread Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (messages.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (initialMessage.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(6.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = initialMessage,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "No previous messages",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(messages) { msg ->
                                PopupBubbleItem(msg = msg)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 3. Quick Response Presets
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    val presets = if (quickResponses.isNotEmpty()) quickResponses.map { it.text } else listOf(
                        "OK 👍",
                        "On my way! 🏃",
                        "Can't talk right now.",
                        "Thanks!",
                        "Call you later."
                    )
                    items(presets) { preset ->
                        AssistChip(
                            onClick = {
                                replyText = preset
                            },
                            label = { Text(preset, fontSize = 12.sp) },
                            shape = RoundedCornerShape(12.dp),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 4. Quick Reply Input Field & Send Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Quick reply...", fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_reply_input_field"),
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank() && !isSending) {
                                isSending = true
                                val textToSend = replyText.trim()
                                scope.launch {
                                    try {
                                        // 1. Send SMS via repository
                                        messageRepository.sendMessage(
                                            recipientPhone = senderPhone,
                                            recipientName = senderName,
                                            content = textToSend
                                        )

                                        // 2. Clear unread count for conversation
                                        messageRepository.clearUnreadCount(conversationId)

                                        // 3. Dismiss system notification from shade since user responded
                                        NotificationManagerCompat.from(context).cancel(senderPhone.hashCode())

                                        // 4. Toast confirmation
                                        Toast.makeText(context, "Reply sent", Toast.LENGTH_SHORT).show()

                                        // 5. Close popup immediately to restore previous app
                                        onDismiss()
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Failed to send SMS", Toast.LENGTH_SHORT).show()
                                        isSending = false
                                    }
                                }
                            }
                        },
                        enabled = replyText.isNotBlank() && !isSending,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("quick_reply_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (replyText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PopupBubbleItem(msg: MessageEntity) {
    val isFromMe = msg.senderPhoneNumber == "ME"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isFromMe) 14.dp else 4.dp,
                bottomEnd = if (isFromMe) 4.dp else 14.dp
            ),
            color = if (isFromMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                Text(
                    text = msg.content,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = TimeFormatter.formatMessageTimestamp(msg.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = (if (isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
