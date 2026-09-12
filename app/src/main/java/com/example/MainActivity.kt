package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
                fontFamily = appSettings.fontFamily
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PulseChatNavHost(
                        initialConversationId = targetConversationId
                    )
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

