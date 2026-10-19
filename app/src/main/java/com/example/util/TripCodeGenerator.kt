package com.example.util

object TripCodeGenerator {
    // Exclude easily confused characters like O, 0, I, 1
    private val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray()

    fun generateCode(hint: String = ""): String {
        val cleanHint = hint.filter { it.isLetter() }.take(4).uppercase()
        val neededRandom = (6 - cleanHint.length).coerceAtLeast(2)
        val randomSuffix = (1..neededRandom).map { CODE_CHARS.random() }.joinToString("")
        val full = (cleanHint + randomSuffix).take(6)
        return if (full.length == 6) full else (1..6).map { CODE_CHARS.random() }.joinToString("")
    }
}
