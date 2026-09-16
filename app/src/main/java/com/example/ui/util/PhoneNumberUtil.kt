package com.example.ui.util

object PhoneNumberUtil {

    /**
     * Normalizes a phone number or contact identifier by stripping non-alphanumeric
     * characters (except leading +), removing formatting spaces, dashes, brackets.
     */
    fun normalize(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        val trimmed = phone.trim()
        
        // Remove spaces, dashes, parentheses, keeping digits and +
        val clean = trimmed.filter { it.isDigit() || it == '+' }
        
        // Extract only digits to check Malawi national number formats
        val digits = clean.filter { it.isDigit() }
        
        if (digits.length == 9) {
            // E.g., "999123456" -> "+265999123456"
            return "+265$digits"
        } else if (digits.length == 10 && digits.startsWith("0")) {
            // E.g., "0999123456" -> "+265999123456" (strip leading 0)
            return "+265${digits.substring(1)}"
        } else if (digits.length == 12 && digits.startsWith("265")) {
            // E.g., "265999123456" or "+265999123456" -> "+265999123456"
            return "+265${digits.substring(3)}"
        }
        
        // Fallback for short codes, services or non-Malawi numbers
        val hasPlus = clean.startsWith("+")
        val digitsAndLetters = clean.filter { it.isLetterOrDigit() }
        return if (hasPlus) "+$digitsAndLetters" else digitsAndLetters
    }

    /**
     * Extracts a core phone suffix (e.g., last 7-10 digits) to allow flexible matching
     * between local numbers (e.g. 5551234) and international numbers (+15551234).
     */
    fun getComparisonKey(phone: String?): String {
        val norm = normalize(phone)
        val digitsOnly = norm.filter { it.isDigit() }
        return if (digitsOnly.length > 7) {
            digitsOnly.takeLast(7)
        } else {
            norm
        }
    }

    /**
     * Checks if two phone numbers or address identifiers represent the same contact.
     */
    fun areSameContact(phone1: String?, phone2: String?): Boolean {
        if (phone1.isNullOrBlank() || phone2.isNullOrBlank()) return false
        val n1 = normalize(phone1)
        val n2 = normalize(phone2)
        if (n1.equals(n2, ignoreCase = true)) return true
        
        val d1 = n1.filter { it.isDigit() }
        val d2 = n2.filter { it.isDigit() }
        if (d1.length >= 7 && d2.length >= 7) {
            return d1.takeLast(7) == d2.takeLast(7)
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
