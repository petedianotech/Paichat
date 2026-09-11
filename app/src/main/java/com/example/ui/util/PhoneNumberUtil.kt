package com.example.ui.util

object PhoneNumberUtil {

    /**
     * Normalizes a phone number or contact identifier by stripping non-alphanumeric
     * characters (except leading +), removing formatting spaces, dashes, brackets.
     */
    fun normalize(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        val trimmed = phone.trim()
        val hasPlus = trimmed.startsWith("+")
        val digitsAndLetters = trimmed.filter { it.isLetterOrDigit() }
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
}
