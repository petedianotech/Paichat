package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.ui.util.AvatarUtil
import com.example.ui.util.PhoneNumberUtil

/**
 * Premium Contact Avatar component supporting:
 * - Real Android contact photos with hardware-accelerated memory/disk caching
 * - Service / Short-code / Automated sender badge icon
 * - Unknown number person silhouette fallback
 * - Vibrant human-designed initials fallback for saved contacts
 */
@Composable
fun ContactAvatar(
    name: String?,
    phoneNumber: String,
    photoUri: String? = null,
    size: Dp = 48.dp,
    customColor: Color? = null,
    fontSize: TextUnit = 16.sp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contactType = remember(phoneNumber, name) {
        PhoneNumberUtil.getContactType(phoneNumber, name)
    }

    val displayName = name?.takeIf { it.isNotBlank() } ?: phoneNumber.ifBlank { "Contact" }
    val initials = remember(displayName) { AvatarUtil.getInitials(displayName) }

    val defaultBgColor = when (contactType) {
        PhoneNumberUtil.ContactType.SERVICE_MESSAGE -> Color(0xFF334155) // Slate dark
        PhoneNumberUtil.ContactType.UNKNOWN_NUMBER -> Color(0xFF475569) // Neutral slate
        PhoneNumberUtil.ContactType.SAVED_CONTACT -> AvatarUtil.getAvatarColor(phoneNumber.ifBlank { displayName })
    }
    val avatarBgColor = customColor ?: defaultBgColor

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarBgColor),
        contentAlignment = Alignment.Center
    ) {
        AvatarFallbackView(
            contactType = contactType,
            initials = initials,
            fontSize = fontSize,
            size = size
        )

        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photoUri)
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = "Avatar for $displayName",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun AvatarFallbackView(
    contactType: PhoneNumberUtil.ContactType,
    initials: String,
    fontSize: TextUnit,
    size: Dp
) {
    val iconSize = (size.value * 0.52f).dp
    when (contactType) {
        PhoneNumberUtil.ContactType.SERVICE_MESSAGE -> {
            Icon(
                imageVector = Icons.Default.Business,
                contentDescription = "Service Message",
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
        PhoneNumberUtil.ContactType.UNKNOWN_NUMBER -> {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Unknown Contact",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(iconSize)
            )
        }
        PhoneNumberUtil.ContactType.SAVED_CONTACT -> {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize,
                letterSpacing = (-0.3).sp
            )
        }
    }
}
