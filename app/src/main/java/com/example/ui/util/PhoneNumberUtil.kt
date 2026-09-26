package com.example.ui.util

object PhoneNumberUtil {

    /**
     * Normalizes a phone number or contact identifier by stripping non-alphanumeric
     * characters (except leading +), removing formatting spaces, dashes, brackets.
     * Safely preserves international E.164 formats, local formats, and alphanumeric sender IDs.
     */
    fun normalize(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        val trimmed = phone.trim()

        // Check if alphanumeric sender (like "GOOGLE", "BANK", "AIRTEL")
        val letters = trimmed.filter { it.isLetter() }
        val digits = trimmed.filter { it.isDigit() }
        val hasPlus = trimmed.startsWith("+")

        if (letters.isNotEmpty() && digits.isEmpty()) {
            return trimmed.uppercase()
        }

        // Clean digits and leading '+'
        val clean = buildString {
            if (hasPlus) append('+')
            append(digits)
        }

        // Malawi (+265) format preservation for local numbers
        if (digits.length == 9 && (digits.startsWith("8") || digits.startsWith("9"))) {
            return "+265$digits"
        } else if (digits.length == 10 && digits.startsWith("0") && (digits[1] == '8' || digits[1] == '9')) {
            return "+265${digits.substring(1)}"
        } else if (digits.length == 12 && digits.startsWith("265")) {
            return "+$digits"
        }

        // Standard number or international format
        return if (clean.isNotBlank()) clean else trimmed
    }

    /**
     * Extracts only clean digits from a phone number string.
     */
    fun extractDigits(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        return phone.filter { it.isDigit() }
    }

    /**
     * Extracts a core phone suffix (last 7 to 9 digits) to allow flexible matching
     * between local numbers and international numbers.
     */
    fun getComparisonKey(phone: String?): String {
        val digits = extractDigits(phone)
        return when {
            digits.length >= 9 -> digits.takeLast(9)
            digits.length >= 7 -> digits.takeLast(7)
            digits.isNotEmpty() -> digits
            else -> phone?.trim()?.lowercase() ?: ""
        }
    }

    /**
     * Checks if two phone numbers or address identifiers represent the same contact.
     * Accurately handles local numbers vs international prefix, spaces, dashes, and short codes.
     */
    fun areSameContact(phone1: String?, phone2: String?): Boolean {
        if (phone1.isNullOrBlank() || phone2.isNullOrBlank()) return false
        val n1 = normalize(phone1)
        val n2 = normalize(phone2)
        if (n1.equals(n2, ignoreCase = true)) return true

        val d1 = extractDigits(phone1)
        val d2 = extractDigits(phone2)
        if (d1.isNotEmpty() && d1 == d2) return true

        // Match international vs national format (e.g. +14155551234 vs 4155551234 or 04155551234)
        if (d1.isNotEmpty() && d2.isNotEmpty()) {
            if (d1.endsWith(d2) || d2.endsWith(d1)) {
                val minLen = minOf(d1.length, d2.length)
                if (minLen >= 7) return true
            }
            val matchLength = minOf(d1.length, d2.length)
            if (matchLength >= 9 && d1.takeLast(matchLength) == d2.takeLast(matchLength)) {
                return true
            }
            if ((d1.length == 7 || d2.length == 7) && d1.takeLast(7) == d2.takeLast(7)) {
                return true
            }
        }
        return false
    }

    /**
     * Determines whether a sender address is an automated service, short code,
     * network notification, or bank/carrier alert.
     */
    fun isServiceMessage(sender: String?): Boolean {
        if (sender.isNullOrBlank()) return false
        val clean = sender.trim()

        // Carrier USSD / short codes (*100#, #123#, etc.)
        if (clean.startsWith("*") || clean.startsWith("#") || clean.endsWith("#")) {
            return true
        }

        // Alphanumeric sender ID (contains letters and no +, e.g., "GOOGLE", "VERIZON", "CHASE")
        val lettersOnly = clean.filter { it.isLetter() }
        val digitsOnly = clean.filter { it.isDigit() }
        if (lettersOnly.isNotEmpty() && !clean.startsWith("+") && !clean.contains("@")) {
            return true
        }

        // Standard telephony short codes (3 to 6 digits, without country code prefix)
        if (digitsOnly.length in 3..6 && !clean.startsWith("+")) {
            return true
        }

        return false
    }

    enum class ContactType {
        SAVED_CONTACT,
        UNKNOWN_NUMBER,
        SERVICE_MESSAGE
    }

    /**
     * Classifies a conversation into a Saved Contact, Unknown Number, or Service Message.
     */
    fun getContactType(phoneNumber: String?, contactName: String?): ContactType {
        if (isServiceMessage(phoneNumber)) {
            return ContactType.SERVICE_MESSAGE
        }
        val cleanName = contactName?.trim()
        val cleanPhone = phoneNumber?.trim()
        if (!cleanName.isNullOrBlank() && cleanName != cleanPhone && !isServiceMessage(cleanName)) {
            return ContactType.SAVED_CONTACT
        }
        return ContactType.UNKNOWN_NUMBER
    }

    /**
     * Helper to detect one-time verification codes or passwords (OTP) in service messages.
     * Extracts 4-8 digit codes typically accompanying keywords like 'code', 'OTP', 'verification'.
     */
    fun extractOtpCode(messageText: String): String? {
        if (messageText.isBlank()) return null
        val lower = messageText.lowercase()
        val hasOtpKeyword = lower.contains("code") ||
                lower.contains("otp") ||
                lower.contains("verify") ||
                lower.contains("verification") ||
                lower.contains("password") ||
                lower.contains("pin") ||
                lower.contains("secret") ||
                lower.contains("passcode")

        if (!hasOtpKeyword) return null

        // Match 4 to 8 consecutive digits
        val regex = Regex("""\b\d{4,8}\b""")
        val match = regex.find(messageText)
        return match?.value
    }
}
