package com.example.ui.profile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mms
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preference.AppSettings
import com.example.ui.theme.AppFonts
import com.example.ui.theme.PrimaryIndigoLight
import com.example.ui.theme.PrimaryLight
import com.example.ui.theme.PrimaryPurpleLight
import com.example.ui.theme.PrimaryRoseLight
import com.example.ui.util.AvatarUtil
import com.example.ui.util.TimeFormatter

enum class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badgeColor: Color? = null
) {
    APPEARANCE("Appearance", "Theme, accent colors, bubble styles, fonts", Icons.Default.Palette),
    MESSAGING("Messaging", "Signature, counters, auto-retry, haptics", Icons.Default.Sms),
    SENDING_DELIVERY("Sending & Delivery", "Delayed send, delivery reports, SIM card", Icons.Default.Send),
    NOTIFICATIONS("Notifications", "Sound, reminders, preview controls", Icons.Default.Notifications),
    POPUP_PREVIEW("Popup Preview", "Floating quick reply over apps, sizes", Icons.Default.AspectRatio),
    SOUNDS_VIBRATION("Sounds & Vibration", "Vibration patterns, alert tones, send haptics", Icons.Default.Vibration),
    PRIVACY("Privacy & Blocking", "Blocked numbers manager, preview privacy", Icons.Default.Security),
    CONTACTS("Contacts", "Directory synchronization, phonebook linking", Icons.Default.Contacts),
    BACKUP_DATA("Backup & Data", "SMS database synchronization, SIM storage", Icons.Default.Storage),
    SMS_HANDLER("SMS Handler & Access", "Default SMS app, permissions, battery saver", Icons.Default.SimCard),
    ADVANCED("Advanced", "Scheduled SMS manager, system diagnostics", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val appSettings by viewModel.appSettings.collectAsState()
    val isDefaultSmsApp by viewModel.isDefaultSmsApp.collectAsState()
    val isBatteryOptimized by viewModel.isBatteryOptimized.collectAsState()
    val hasSmsPermission by viewModel.hasSmsPermission.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val blockedContacts by viewModel.blockedContacts.collectAsState()
    val allScheduledMessages by viewModel.allScheduledMessages.collectAsState()

    var selectedCategory by remember { mutableStateOf<SettingsCategory?>(null) }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var signatureInput by remember { mutableStateOf(appSettings.signatureText) }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(appSettings.userName) }
    var phoneInput by remember { mutableStateOf(appSettings.userPhoneNumber) }
    var statusInput by remember { mutableStateOf(appSettings.userStatus) }
    var selectedAvatarColor by remember { mutableStateOf(appSettings.userAvatarColor) }

    val userAvatarColorParsed = remember(appSettings.userAvatarColor) {
        try { Color(android.graphics.Color.parseColor(appSettings.userAvatarColor)) } catch (_: Exception) { PrimaryLight }
    }

    val smsRoleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkStatus(context)
    }

    val batteryOptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.checkStatus(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkStatus(context)
        viewModel.syncDeviceData(context)
    }

    LaunchedEffect(Unit) {
        viewModel.checkStatus(context)
    }

    LaunchedEffect(syncMessage) {
        syncMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSyncMessage()
        }
    }

    // Handle system back gesture
    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedCategory?.title ?: "Settings",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedCategory != null) {
                            selectedCategory = null
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedCategory == null) {
                // Main Settings Category Index
                MainSettingsIndex(
                    appSettings = appSettings,
                    userAvatarColor = userAvatarColorParsed,
                    isDefaultSmsApp = isDefaultSmsApp,
                    hasSmsPermission = hasSmsPermission,
                    isBatteryOptimized = isBatteryOptimized,
                    blockedCount = blockedContacts.size,
                    scheduledCount = allScheduledMessages.size,
                    onEditProfileClick = {
                        nameInput = appSettings.userName
                        phoneInput = appSettings.userPhoneNumber
                        statusInput = appSettings.userStatus
                        selectedAvatarColor = appSettings.userAvatarColor
                        showEditProfileDialog = true
                    },
                    onCategorySelect = { category ->
                        selectedCategory = category
                    },
                    onSetDefaultSms = {
                        val intent = viewModel.createDefaultSmsIntent(context)
                        if (intent != null) smsRoleLauncher.launch(intent)
                    },
                    onGrantPermissions = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.SEND_SMS,
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_SMS,
                                Manifest.permission.READ_CONTACTS
                            )
                        )
                    }
                )
            } else {
                // Category Specific Sub-Page
                CategorySubPage(
                    category = selectedCategory!!,
                    appSettings = appSettings,
                    viewModel = viewModel,
                    isDefaultSmsApp = isDefaultSmsApp,
                    hasSmsPermission = hasSmsPermission,
                    isBatteryOptimized = isBatteryOptimized,
                    blockedContacts = blockedContacts,
                    allScheduledMessages = allScheduledMessages,
                    smsRoleLauncher = smsRoleLauncher,
                    batteryOptLauncher = batteryOptLauncher,
                    permissionLauncher = permissionLauncher,
                    onOpenSignatureDialog = {
                        signatureInput = appSettings.signatureText
                        showSignatureDialog = true
                    }
                )
            }
        }
    }

    // Signature Edit Dialog
    if (showSignatureDialog) {
        AlertDialog(
            onDismissRequest = { showSignatureDialog = false },
            title = { Text("Edit SMS Signature", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "This text will be automatically appended to every SMS message you send.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = signatureInput,
                        onValueChange = { signatureInput = it },
                        placeholder = { Text("e.g. — Sent with PaiChat") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setSignatureText(signatureInput)
                        showSignatureDialog = false
                    },
                    shape = CircleShape
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignatureDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        val avatarPalette = listOf(
            "#005AC1" to "Blue",
            "#4F46E5" to "Indigo",
            "#7C3AED" to "Violet",
            "#9333EA" to "Purple",
            "#E11D48" to "Rose",
            "#0891B2" to "Teal",
            "#EA580C" to "Amber",
            "#475569" to "Slate"
        )

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile & Identity", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val previewColor = try { Color(android.graphics.Color.parseColor(selectedAvatarColor)) } catch (_: Exception) { PrimaryLight }
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .align(Alignment.CenterHorizontally)
                            .clip(CircleShape)
                            .background(previewColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = AvatarUtil.getInitials(nameInput),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text("Avatar Color:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        avatarPalette.take(4).forEach { (hex, _) ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { PrimaryLight }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { selectedAvatarColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedAvatarColor.equals(hex, ignoreCase = true)) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        avatarPalette.drop(4).forEach { (hex, _) ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { PrimaryLight }
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { selectedAvatarColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedAvatarColor.equals(hex, ignoreCase = true)) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Display Name") },
                        placeholder = { Text("Your Name or Nickname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    )

                    OutlinedTextField(
                        value = statusInput,
                        onValueChange = { statusInput = it },
                        label = { Text("Status / Bio") },
                        placeholder = { Text("e.g. Available, At work, In meetings") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number / SIM Line") },
                        placeholder = { Text("e.g. +1 555-0199") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setUserName(nameInput.trim())
                        viewModel.setUserPhoneNumber(phoneInput.trim())
                        viewModel.setUserStatus(statusInput.trim())
                        viewModel.setUserAvatarColor(selectedAvatarColor)
                        showEditProfileDialog = false
                    },
                    shape = CircleShape
                ) {
                    Text("Save Profile")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Main Settings Index showing User Profile & Organized Category Rows.
 */
@Composable
private fun MainSettingsIndex(
    appSettings: AppSettings,
    userAvatarColor: Color,
    isDefaultSmsApp: Boolean,
    hasSmsPermission: Boolean,
    isBatteryOptimized: Boolean,
    blockedCount: Int,
    scheduledCount: Int,
    onEditProfileClick: () -> Unit,
    onCategorySelect: (SettingsCategory) -> Unit,
    onSetDefaultSms: () -> Unit,
    onGrantPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Profile & Identity Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditProfileClick() }
                .testTag("settings_profile_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(userAvatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AvatarUtil.getInitials(appSettings.userName),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appSettings.userName.ifBlank { "You" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = appSettings.userStatus.ifBlank { "PaiChat SMS Messenger" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (appSettings.userPhoneNumber.isNotBlank()) {
                        Text(
                            text = appSettings.userPhoneNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(onClick = onEditProfileClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Quick Alert Banner if not default SMS app or permissions missing
        if (!isDefaultSmsApp || !hasSmsPermission) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (!isDefaultSmsApp) "PaiChat is not your default SMS app" else "SMS permissions required for messaging",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    TextButton(
                        onClick = if (!isDefaultSmsApp) onSetDefaultSms else onGrantPermissions
                    ) {
                        Text(
                            text = if (!isDefaultSmsApp) "Set Default" else "Grant",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // 2. Settings Category Groups
        SettingsGroupCard {
            SettingsCategoryRow(
                category = SettingsCategory.APPEARANCE,
                subtitle = "Theme (${appSettings.themeMode.lowercase().replaceFirstChar { it.uppercase() }}), Font (${appSettings.fontFamily})",
                onClick = { onCategorySelect(SettingsCategory.APPEARANCE) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
            SettingsCategoryRow(
                category = SettingsCategory.MESSAGING,
                subtitle = if (appSettings.signatureText.isNotBlank()) "Signature enabled" else "Signature, counter, haptics",
                onClick = { onCategorySelect(SettingsCategory.MESSAGING) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
            SettingsCategoryRow(
                category = SettingsCategory.SENDING_DELIVERY,
                subtitle = "Delay: ${appSettings.sendDelaySeconds}s, Reports: ${if (appSettings.deliveryReports) "On" else "Off"}",
                onClick = { onCategorySelect(SettingsCategory.SENDING_DELIVERY) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
            SettingsCategoryRow(
                category = SettingsCategory.NOTIFICATIONS,
                subtitle = "Sound: ${if (appSettings.notificationSound) "On" else "Off"}, Vibrate: ${appSettings.notificationVibratePattern}",
                onClick = { onCategorySelect(SettingsCategory.NOTIFICATIONS) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
            SettingsCategoryRow(
                category = SettingsCategory.POPUP_PREVIEW,
                subtitle = if (appSettings.quickReplyPopup) "Enabled (${appSettings.popupPreviewSize.lowercase()})" else "Disabled",
                onClick = { onCategorySelect(SettingsCategory.POPUP_PREVIEW) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
            SettingsCategoryRow(
                category = SettingsCategory.SOUNDS_VIBRATION,
                subtitle = "Alert sounds, haptic feedback patterns",
                onClick = { onCategorySelect(SettingsCategory.SOUNDS_VIBRATION) }
            )
        }

        SettingsGroupCard {
            SettingsCategoryRow(
                category = SettingsCategory.PRIVACY,
                subtitle = if (blockedCount > 0) "$blockedCount blocked numbers" else "Blocked numbers & message masking",
                onClick = { onCategorySelect(SettingsCategory.PRIVACY) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
            SettingsCategoryRow(
                category = SettingsCategory.CONTACTS,
                subtitle = "Contact directory & name formatting",
                onClick = { onCategorySelect(SettingsCategory.CONTACTS) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
            SettingsCategoryRow(
                category = SettingsCategory.BACKUP_DATA,
                subtitle = "Sync SIM messages, database info",
                onClick = { onCategorySelect(SettingsCategory.BACKUP_DATA) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
            SettingsCategoryRow(
                category = SettingsCategory.SMS_HANDLER,
                subtitle = if (isDefaultSmsApp) "Default app active" else "Default app not set",
                onClick = { onCategorySelect(SettingsCategory.SMS_HANDLER) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
            SettingsCategoryRow(
                category = SettingsCategory.ADVANCED,
                subtitle = if (scheduledCount > 0) "$scheduledCount scheduled SMS" else "Scheduled SMS, diagnostics & reset",
                onClick = { onCategorySelect(SettingsCategory.ADVANCED) }
            )
        }

        // Developer Credits Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_developer_credits_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Developer Credits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "PaiChat is developed and maintained with passion by Peter Damiano.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        try {
                            uriHandler.openUri("https://peterdamiano.vercel.app")
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.align(Alignment.End),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = CircleShape
                ) {
                    Text("Visit Portfolio", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Category Sub-Page displaying categorized, clean settings rows.
 */
@Composable
private fun CategorySubPage(
    category: SettingsCategory,
    appSettings: AppSettings,
    viewModel: ProfileViewModel,
    isDefaultSmsApp: Boolean,
    hasSmsPermission: Boolean,
    isBatteryOptimized: Boolean,
    blockedContacts: List<com.example.data.local.entity.BlockedContactEntity>,
    allScheduledMessages: List<com.example.data.local.entity.ScheduledMessageEntity>,
    smsRoleLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    batteryOptLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    onOpenSignatureDialog: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (category) {
            SettingsCategory.APPEARANCE -> {
                // Appearance Category
                Text("Theme & Colors", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                SettingsGroupCard {
                    // Theme Mode
                    SettingsRow(
                        title = "Theme Mode",
                        subtitle = "Select light, dark, or AMOLED deep black"
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("SYSTEM" to "Auto", "LIGHT" to "Light", "DARK" to "Dark", "AMOLED" to "AMOLED").forEach { (mode, label) ->
                                FilterChip(
                                    selected = appSettings.themeMode == mode,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    label = { Text(label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Accent Color
                    SettingsRow(
                        title = "Accent Color",
                        subtitle = "Theme highlights and primary button tint"
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val colorOptions = listOf(
                                "BLUE" to PrimaryLight,
                                "INDIGO" to PrimaryIndigoLight,
                                "PURPLE" to PrimaryPurpleLight,
                                "ROSE" to PrimaryRoseLight,
                                "TEAL" to Color(0xFF006874),
                                "AMBER" to Color(0xFF855300)
                            )
                            colorOptions.forEach { (themeKey, color) ->
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { viewModel.setColorTheme(themeKey) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (appSettings.colorTheme == themeKey) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Text("Typography & Shapes", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                SettingsGroupCard {
                    // Bubble Shape
                    SettingsRow(
                        title = "Chat Bubble Shape",
                        subtitle = "Bubble curvature in conversation threads"
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("ROUNDED" to "Rounded", "PILL" to "Pill", "SQUARE" to "Square").forEach { (shape, label) ->
                                FilterChip(
                                    selected = appSettings.bubbleShape == shape,
                                    onClick = { viewModel.setBubbleShape(shape) },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Font Size
                    SettingsRow(
                        title = "Text Size",
                        subtitle = "Scale message and interface text"
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("SMALL" to "Small", "NORMAL" to "Normal", "LARGE" to "Large", "EXTRA_LARGE" to "XL").forEach { (size, label) ->
                                FilterChip(
                                    selected = appSettings.fontSize == size,
                                    onClick = { viewModel.setFontSize(size) },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Font Family
                    SettingsRow(
                        title = "App Font Family",
                        subtitle = "Choose clean typography style"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AppFonts.fontsList.forEach { option ->
                                FilterChip(
                                    selected = appSettings.fontFamily == option.key,
                                    onClick = { viewModel.setFontFamily(option.key) },
                                    label = {
                                        Text(
                                            text = option.displayName,
                                            fontFamily = AppFonts.getFontFamily(option.key),
                                            fontSize = 12.sp,
                                            fontWeight = if (appSettings.fontFamily == option.key) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            SettingsCategory.MESSAGING -> {
                // Messaging Category
                Text("Composer & Drafting", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                SettingsGroupCard {
                    // Signature
                    SettingsRow(
                        title = "SMS Signature",
                        subtitle = if (appSettings.signatureText.isNotBlank()) appSettings.signatureText else "Tap to add signature text",
                        trailing = {
                            OutlinedButton(
                                onClick = onOpenSignatureDialog,
                                shape = CircleShape
                            ) {
                                Text(if (appSettings.signatureText.isNotBlank()) "Edit" else "Set")
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Character Counter
                    SettingsRow(
                        title = "Character & SMS Part Counter",
                        subtitle = "Display character count and multi-part SMS split calculations",
                        trailing = {
                            Switch(
                                checked = appSettings.showCharacterCounter,
                                onCheckedChange = { viewModel.setShowCharacterCounter(it) }
                            )
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Auto-Retry Delayed SMS
                    SettingsRow(
                        title = "Auto-Retry Delayed Messages",
                        subtitle = "Automatically re-attempt send if an SMS remains pending over 60s",
                        trailing = {
                            Switch(
                                checked = appSettings.autoRetryAfterTimeout,
                                onCheckedChange = { viewModel.setAutoRetryAfterTimeout(it) }
                            )
                        }
                    )
                }
            }

            SettingsCategory.SENDING_DELIVERY -> {
                // Sending & Delivery Category
                Text("Send Controls", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                SettingsGroupCard {
                    // Delayed Send (Undo Send)
                    SettingsRow(
                        title = "Delayed Send (Undo Send)",
                        subtitle = "Cancellation window before SMS is transmitted"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(0 to "Off", 1 to "1s", 2 to "2s", 3 to "3s", 5 to "5s", 10 to "10s").forEach { (sec, label) ->
                                FilterChip(
                                    selected = appSettings.sendDelaySeconds == sec,
                                    onClick = { viewModel.setSendDelaySeconds(sec) },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Delivery Reports
                    SettingsRow(
                        title = "Delivery Reports",
                        subtitle = "Confirmation when SMS is received by recipient carrier",
                        trailing = {
                            Switch(
                                checked = appSettings.deliveryReports && appSettings.deliveryReportMode != "OFF",
                                onCheckedChange = { viewModel.setDeliveryReports(it) }
                            )
                        }
                    )

                    if (appSettings.deliveryReports && appSettings.deliveryReportMode != "OFF") {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                        SettingsRow(
                            title = "Delivery Report Style",
                            subtitle = "Visual indicators in conversation and shade"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val modes = listOf(
                                    "BOTH" to "Both: Double Marks in Chat & Notification",
                                    "MARKS_ONLY" to "Double Checkmarks in Chat Only",
                                    "NOTIFICATIONS_ONLY" to "Status Bar Notification Alerts Only"
                                )
                                modes.forEach { (modeKey, label) ->
                                    FilterChip(
                                        selected = appSettings.deliveryReportMode == modeKey,
                                        onClick = { viewModel.setDeliveryReportMode(modeKey) },
                                        label = { Text(label, fontSize = 12.sp) },
                                        leadingIcon = if (appSettings.deliveryReportMode == modeKey) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SettingsCategory.NOTIFICATIONS -> {
                // Notifications Category
                Text("Notification Tone & Behavior", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                SettingsGroupCard {
                    // Notification Sound
                    SettingsRow(
                        title = "Notification Sound",
                        subtitle = "Play sound alert on incoming messages",
                        trailing = {
                            Switch(
                                checked = appSettings.notificationSound,
                                onCheckedChange = { viewModel.setNotificationSound(it) }
                            )
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Repeat Notification Reminder
                    SettingsRow(
                        title = "Repeat Notification Reminder",
                        subtitle = "Re-alert periodically if unread messages remain unopened"
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0 to "Off", 1 to "1x", 2 to "2x", 3 to "3x").forEach { (count, label) ->
                                FilterChip(
                                    selected = appSettings.repeatNotificationCount == count,
                                    onClick = { viewModel.setRepeatNotificationCount(count) },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }

            SettingsCategory.POPUP_PREVIEW -> {
                // Popup Preview Category (Textra-style quick reply popup)
                Text("Quick Reply Popup Overlay", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                SettingsGroupCard {
                    // Quick Reply Popup Toggle
                    SettingsRow(
                        title = "Quick Reply Popup Window",
                        subtitle = "Show floating reply window over other apps on incoming SMS",
                        trailing = {
                            Switch(
                                checked = appSettings.quickReplyPopup,
                                onCheckedChange = { viewModel.setQuickReplyPopup(it) }
                            )
                        }
                    )

                    if (appSettings.quickReplyPopup) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                        // Popup Size
                        SettingsRow(
                            title = "Popup Window Dimensions",
                            subtitle = "Adjust size of floating overlay card"
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("COMPACT" to "Compact", "STANDARD" to "Standard", "LARGE" to "Large").forEach { (size, label) ->
                                    FilterChip(
                                        selected = appSettings.popupPreviewSize == size,
                                        onClick = { viewModel.setPopupPreviewSize(size) },
                                        label = { Text(label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                        // Overlay Permission
                        val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Settings.canDrawOverlays(context)
                        } else true

                        SettingsRow(
                            title = "Appear on Top Permission",
                            subtitle = if (canOverlay) "Granted — Popup appears seamlessly over active apps" else "Permission required to show floating reply over apps",
                            trailing = {
                                if (!canOverlay && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    OutlinedButton(
                                        onClick = {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                        },
                                        shape = CircleShape
                                    ) {
                                        Text("Grant", fontSize = 12.sp)
                                    }
                                } else {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                        // Persistence Guidance note
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Closing the popup or tapping outside leaves the notification in your notification shade until marked as read or swiped.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            SettingsCategory.SOUNDS_VIBRATION -> {
                // Sounds & Vibration Category
                Text("Vibration & Haptics", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                SettingsGroupCard {
                    // Vibration Pattern
                    SettingsRow(
                        title = "Incoming SMS Vibration Pattern",
                        subtitle = "Select haptic cadence for received texts"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("DEFAULT" to "Normal", "SHORT" to "Short", "LONG" to "Long", "DOUBLE" to "Double", "OFF" to "Silent").forEach { (pat, label) ->
                                FilterChip(
                                    selected = appSettings.notificationVibratePattern == pat,
                                    onClick = { viewModel.setNotificationVibratePattern(pat) },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Vibrate on Send
                    SettingsRow(
                        title = "Vibrate on Send",
                        subtitle = "Short haptic confirmation when message is dispatched",
                        trailing = {
                            Switch(
                                checked = appSettings.vibrateOnSend,
                                onCheckedChange = { viewModel.setVibrateOnSend(it) }
                            )
                        }
                    )
                }
            }

            SettingsCategory.PRIVACY -> {
                // Privacy & Blocking Category
                Text("Blocked Contacts (${blockedContacts.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (blockedContacts.isEmpty()) {
                    SettingsGroupCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "No blocked contacts. You can block any conversation from its thread options menu.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    SettingsGroupCard {
                        blockedContacts.forEachIndexed { index, blocked ->
                            SettingsRow(
                                title = blocked.contactName ?: blocked.phoneNumber,
                                subtitle = blocked.phoneNumber,
                                trailing = {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.unblockContact(blocked.phoneNumber)
                                            Toast.makeText(context, "Unblocked ${blocked.phoneNumber}", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = CircleShape
                                    ) {
                                        Text("Unblock")
                                    }
                                }
                            )
                            if (index < blockedContacts.size - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }

            SettingsCategory.CONTACTS -> {
                // Contacts Category
                Text("Phonebook & Directory", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                SettingsGroupCard {
                    SettingsRow(
                        title = "Sync Contacts Directory",
                        subtitle = "Re-index device contacts and link with SMS threads",
                        trailing = {
                            Button(
                                onClick = {
                                    if (hasSmsPermission) {
                                        viewModel.syncDeviceData(context)
                                    } else {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_CONTACTS,
                                                Manifest.permission.READ_SMS
                                            )
                                        )
                                    }
                                },
                                shape = CircleShape
                            ) {
                                Text("Sync")
                            }
                        }
                    )
                }
            }

            SettingsCategory.BACKUP_DATA -> {
                // Backup & Data Category
                Text("SMS Database & Storage", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                SettingsGroupCard {
                    SettingsRow(
                        title = "Re-Sync SIM & Device Messages",
                        subtitle = "Scan device SMS inbox and restore missing message history",
                        trailing = {
                            Button(
                                onClick = { viewModel.syncDeviceData(context) },
                                shape = CircleShape
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Now")
                            }
                        }
                    )
                }
            }

            SettingsCategory.SMS_HANDLER -> {
                // SMS Handler & System Permissions Category
                Text("System Telephony & Handler Status", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                SettingsGroupCard {
                    // Default SMS App
                    SettingsRow(
                        title = "Default SMS App",
                        subtitle = if (isDefaultSmsApp) "PaiChat is set as active default SMS handler" else "Required to receive and send text messages directly",
                        trailing = {
                            if (isDefaultSmsApp) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Button(
                                    onClick = {
                                        val intent = viewModel.createDefaultSmsIntent(context)
                                        if (intent != null) smsRoleLauncher.launch(intent)
                                    },
                                    shape = CircleShape
                                ) {
                                    Text("Set Default")
                                }
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                    // SMS Permissions
                    SettingsRow(
                        title = "SMS & Contacts Permissions",
                        subtitle = if (hasSmsPermission) "All telephony permissions granted" else "SMS, MMS and Contact permissions required",
                        trailing = {
                            if (hasSmsPermission) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.SEND_SMS,
                                                Manifest.permission.RECEIVE_SMS,
                                                Manifest.permission.READ_SMS,
                                                Manifest.permission.READ_CONTACTS
                                            )
                                        )
                                    },
                                    shape = CircleShape
                                ) {
                                    Text("Grant")
                                }
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Battery Saver / Background execution
                    SettingsRow(
                        title = "Background Execution Reliability",
                        subtitle = if (!isBatteryOptimized) "Unrestricted background execution enabled" else "Exempt from OS battery saver for instant SMS receipt",
                        trailing = {
                            if (!isBatteryOptimized) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            } else {
                                Button(
                                    onClick = {
                                        val intent = viewModel.createRequestBatteryOptimizationIntent(context)
                                        if (intent != null) {
                                            try {
                                                batteryOptLauncher.launch(intent)
                                            } catch (_: Exception) {
                                                context.startActivity(viewModel.createAppSettingsIntent(context))
                                            }
                                        } else {
                                            context.startActivity(viewModel.createAppSettingsIntent(context))
                                        }
                                    },
                                    shape = CircleShape
                                ) {
                                    Text("Allow")
                                }
                            }
                        }
                    )
                }
            }

            SettingsCategory.ADVANCED -> {
                // Advanced Category (Scheduled Messages, Reset)
                Text("Scheduled Messages (${allScheduledMessages.size})", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (allScheduledMessages.isEmpty()) {
                    SettingsGroupCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "No pending scheduled messages. Schedule messages from the conversation attachment menu (+).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    SettingsGroupCard {
                        allScheduledMessages.forEachIndexed { index, scheduled ->
                            SettingsRow(
                                title = "To: ${scheduled.recipientName ?: scheduled.recipientPhoneNumber}",
                                subtitle = "\"${scheduled.content}\"\nScheduled: ${TimeFormatter.formatMessageTimestamp(scheduled.scheduledTimestamp)}",
                                trailing = {
                                    Row {
                                        IconButton(onClick = { viewModel.sendScheduledMessageNow(scheduled) }) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send now", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { viewModel.cancelScheduledMessage(scheduled.id) }) {
                                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            )
                            if (index < allScheduledMessages.size - 1) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Clean iOS/Modern Android Grouped Surface Card
 */
@Composable
private fun SettingsGroupCard(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/**
 * Clean Settings Row with Title, Short Description, and Control
 */
@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    bottomControl: (@Composable () -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(16.dp)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailing()
            }
        }

        if (bottomControl != null) {
            Spacer(modifier = Modifier.height(10.dp))
            bottomControl()
        }
    }
}

/**
 * Category Navigation Row in Main Index
 */
@Composable
private fun SettingsCategoryRow(
    category: SettingsCategory,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(14.dp)
        )
    }
}
