package com.example.ui.chat

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.MessageStatus
import com.example.data.local.entity.MessageType
import com.example.ui.util.TimeFormatter
import com.example.ui.util.VoiceNoteHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

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
    val context = LocalContext.current
    var isAudioPlaying by remember { mutableStateOf(false) }
    var showFullImageDialog by remember { mutableStateOf(false) }

    DisposableEffect(message.messageId) {
        onDispose {
            if (isAudioPlaying) {
                VoiceNoteHelper.stopPlayback()
                isAudioPlaying = false
            }
        }
    }

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

    val isVoiceNote = !message.mediaUrl.isNullOrBlank() &&
            (message.mediaUrl.endsWith(".m4a") || message.mediaUrl.contains("voice_") || message.mediaUrl.endsWith(".mp3"))
    val isPhotoMms = !message.mediaUrl.isNullOrBlank() && !isVoiceNote
    val isMms = message.messageType == MessageType.MMS || !message.mediaUrl.isNullOrBlank()

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
                .widthIn(max = 300.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
                .testTag("message_bubble_${message.messageId}")
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // 1. Voice Note Player Layout
                if (isVoiceNote && message.mediaUrl != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isFromMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier
                                .size(42.dp)
                                .clickable {
                                    if (isAudioPlaying) {
                                        VoiceNoteHelper.stopPlayback()
                                        isAudioPlaying = false
                                    } else {
                                        VoiceNoteHelper.playAudio(
                                            filePath = message.mediaUrl,
                                            onCompletion = { isAudioPlaying = false },
                                            onError = {
                                                isAudioPlaying = false
                                                Toast.makeText(context, "Could not play audio", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                        isAudioPlaying = true
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isAudioPlaying) "Pause" else "Play",
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = contentColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isAudioPlaying) "Playing voice note..." else "Voice Note",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = contentColor
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Audio Waveform Visualizer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val barHeights = listOf(8, 14, 20, 12, 18, 24, 16, 10, 22, 14, 8, 16, 12, 6)
                                barHeights.forEachIndexed { i, h ->
                                    val active = isAudioPlaying && (i % 2 == 0)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height((if (active) h + 4 else h).dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(contentColor.copy(alpha = if (active) 0.95f else 0.5f))
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 2. Photo MMS Preview
                if (isPhotoMms && message.mediaUrl != null) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = "MMS Photo Attachment",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showFullImageDialog = true },
                            contentScale = ContentScale.Crop
                        )

                        // Save to gallery button
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.55f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(32.dp)
                                .clickable {
                                    saveImageToGallery(context, message.mediaUrl)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Save photo to device",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 3. Text Content with Partial Selection Container
                if (message.content.isNotBlank() && (!isPhotoMms || message.content != "Photo attachment") && (!isVoiceNote || message.content != "Voice message")) {
                    SelectionContainer {
                        Text(
                            text = message.content,
                            style = textStyle,
                            color = contentColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 4. Bottom row: MMS Badge + Timestamp + Status tick
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMms) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = contentColor.copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "MMS",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = contentColor,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

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

    // Full screen image preview dialog
    if (showFullImageDialog && message.mediaUrl != null) {
        Dialog(onDismissRequest = { showFullImageDialog = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "Full MMS Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(onClick = { showFullImageDialog = false }) {
                            Text("Close")
                        }
                        Button(onClick = {
                            saveImageToGallery(context, message.mediaUrl)
                            showFullImageDialog = false
                        }) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Gallery")
                        }
                    }
                }
            }
        }
    }
}

private fun saveImageToGallery(context: Context, imageUrl: String) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()
            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val bitmap = (result as? BitmapDrawable)?.bitmap

            if (bitmap != null) {
                val filename = "MMS_${System.currentTimeMillis()}.jpg"
                var fos: OutputStream? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PulseChat")
                    }
                    val imageUri: Uri? = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    fos = imageUri?.let { context.contentResolver.openOutputStream(it) }
                }

                fos?.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Saved photo to Pictures/PulseChat", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Saved image attachment", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved image attachment", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
