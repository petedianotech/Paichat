package com.example.ui.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import java.util.regex.Pattern

private val URL_PATTERN = Pattern.compile(
    "(https?://[\\w\\d:#@%/;$()~_?\\+-=\\\\.&]+)|(www\\.[\\w\\d:#@%/;$()~_?\\+-=\\\\.&]+)",
    Pattern.CASE_INSENSITIVE
)

private val EMAIL_PATTERN = Pattern.compile(
    "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}",
    Pattern.CASE_INSENSITIVE
)

private val PHONE_PATTERN = Pattern.compile(
    "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}",
    Pattern.CASE_INSENSITIVE
)

private data class LinkSpan(
    val start: Int,
    val end: Int,
    val url: String,
    val type: LinkType
)

private enum class LinkType {
    URL, EMAIL, PHONE
}

@Composable
fun ContextualMessageText(
    text: String,
    style: TextStyle,
    color: Color,
    highlightQuery: String = "",
    isFromMe: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val linkColor = if (isFromMe) {
        Color.White.copy(alpha = 0.95f)
    } else {
        MaterialTheme.colorScheme.primary
    }

    val (annotatedString, linkSpans) = remember(text, highlightQuery, color, linkColor) {
        val spans = mutableListOf<LinkSpan>()

        // 1. Detect URLs
        val urlMatcher = URL_PATTERN.matcher(text)
        while (urlMatcher.find()) {
            val matched = urlMatcher.group()
            val fullUrl = if (matched.startsWith("http://", true) || matched.startsWith("https://", true)) {
                matched
            } else {
                "https://$matched"
            }
            spans.add(LinkSpan(urlMatcher.start(), urlMatcher.end(), fullUrl, LinkType.URL))
        }

        // 2. Detect Emails
        val emailMatcher = EMAIL_PATTERN.matcher(text)
        while (emailMatcher.find()) {
            val email = emailMatcher.group()
            spans.add(LinkSpan(emailMatcher.start(), emailMatcher.end(), "mailto:$email", LinkType.EMAIL))
        }

        // 3. Detect Phone numbers
        val phoneMatcher = PHONE_PATTERN.matcher(text)
        while (phoneMatcher.find()) {
            val phone = phoneMatcher.group()
            // Ensure no overlap with URLs
            val overlaps = spans.any { s -> phoneMatcher.start() < s.end && phoneMatcher.end() > s.start }
            if (!overlaps && phone.length >= 7) {
                spans.add(LinkSpan(phoneMatcher.start(), phoneMatcher.end(), "tel:$phone", LinkType.PHONE))
            }
        }

        // Build annotated string
        val builder = buildAnnotatedString {
            var currentIndex = 0
            val sortedSpans = spans.sortedBy { it.start }

            // Apply text and styles
            for (span in sortedSpans) {
                if (span.start > currentIndex) {
                    appendHighlightableText(
                        text = text.substring(currentIndex, span.start),
                        query = highlightQuery,
                        baseColor = color
                    )
                }

                val linkText = text.substring(span.start, span.end)
                pushStringAnnotation(tag = span.type.name, annotation = span.url)
                withStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append(linkText)
                }
                pop()

                currentIndex = span.end
            }

            if (currentIndex < text.length) {
                appendHighlightableText(
                    text = text.substring(currentIndex),
                    query = highlightQuery,
                    baseColor = color
                )
            }
        }

        Pair(builder, spans)
    }

    SelectionContainer {
        Text(
            text = annotatedString,
            style = style,
            color = color,
            modifier = modifier
        )
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendHighlightableText(
    text: String,
    query: String,
    baseColor: Color
) {
    if (query.isBlank() || !text.contains(query, ignoreCase = true)) {
        append(text)
        return
    }

    val queryLower = query.lowercase()
    val textLower = text.lowercase()
    var startIndex = 0

    while (startIndex < text.length) {
        val index = textLower.indexOf(queryLower, startIndex)
        if (index == -1) {
            append(text.substring(startIndex))
            break
        }
        if (index > startIndex) {
            append(text.substring(startIndex, index))
        }
        val matchEnd = index + query.length
        withStyle(
            style = SpanStyle(
                background = Color(0xFFFBBF24),
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        ) {
            append(text.substring(index, matchEnd))
        }
        startIndex = matchEnd
    }
}
