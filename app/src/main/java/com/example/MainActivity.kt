package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.preference.UserPreferences
import com.example.ui.navigation.PulseChatNavHost
import com.example.ui.theme.PaiChatTheme

class MainActivity : ComponentActivity() {

    private var targetConversationId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        targetConversationId = extractConversationId(intent)

        setContent {
            val userPreferences = remember { UserPreferences(applicationContext) }
            val appSettings by userPreferences.appSettings.collectAsState()

            PaiChatTheme(
                themeMode = appSettings.themeMode,
                colorTheme = appSettings.colorTheme,
                fontFamily = appSettings.fontFamily,
                fontSize = appSettings.fontSize
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PulseChatNavHost(
                        initialConversationId = targetConversationId
                    )

                    if (!appSettings.isTermsAccepted) {
                        TermsAgreementDialog(
                            onAccept = {
                                userPreferences.setTermsAccepted(true)
                            },
                            onDecline = {
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val convId = extractConversationId(intent)
        if (!convId.isNullOrBlank()) {
            targetConversationId = convId
        }
    }

    private fun extractConversationId(intent: Intent?): String? {
        if (intent == null) return null
        val directExtra = intent.getStringExtra("conversationId") ?: intent.getStringExtra("senderPhone")
        if (!directExtra.isNullOrBlank()) return directExtra

        val data: Uri? = intent.data
        if (data != null) {
            val scheme = data.scheme
            if (scheme in listOf("sms", "smsto", "mms", "mmsto")) {
                val ssp = data.schemeSpecificPart
                if (!ssp.isNullOrBlank()) {
                    return ssp.substringBefore('?')
                }
            }
        }
        return null
    }
}

@Composable
fun TermsAgreementDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = {}, // Force explicit action
        title = {
            Text(
                text = "Welcome to PaiChat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Terms & Privacy Policy:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "1. Local Storage:\nAll message databases and system configurations are processed and stored locally on your device safely.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "2. Complete Privacy:\nPaiChat does NOT collect, upload, or share your messages, phone numbers, contact records, or any personal transcripts with external servers or third-parties.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "3. Requested Permissions:\nThe app requests SMS, MMS, and Contacts permissions solely to send/receive texts and show your friends' names correctly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "By clicking Agree, you accept these terms and agree to use the application in compliance with local regulations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept
            ) {
                Text("Agree & Accept")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDecline
            ) {
                Text("Decline & Exit")
            }
        }
    )
}

