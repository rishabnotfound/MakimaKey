package com.makimakey.crypto

/**
 * RFC 4648 compliant Base32 decoder for TOTP secrets
 * Handles both standard Base32 and Base32 without padding
 */
object Base32Decoder {
    private const val BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val DECODE_TABLE = IntArray(128) { -1 }

    init {
        BASE32_CHARS.forEachIndexed { index, char ->
            DECODE_TABLE[char.code] = index
            DECODE_TABLE[char.lowercaseChar().code] = index
        }
    }

    /**
     * Decodes a Base32 encoded string to a byte array
     * @param input Base32 encoded string (with or without padding)
     * @return Decoded byte array
     * @throws IllegalArgumentException if input contains invalid characters
     */
    fun decode(input: String): ByteArray {
        val sanitized = input.replace("=", "").replace(" ", "").replace("-", "")

        if (sanitized.isEmpty()) {
            return ByteArray(0)
        }

        val outputLength = (sanitized.length * 5) / 8
        val output = ByteArray(outputLength)

        var buffer = 0L
        var bitsInBuffer = 0
        var outputIndex = 0

        for (char in sanitized) {
            val value = DECODE_TABLE[char.code]
            if (value == -1) {
                throw IllegalArgumentException("Invalid Base32 character: $char")
            }

            buffer = (buffer shl 5) or value.toLong()
            bitsInBuffer += 5

            if (bitsInBuffer >= 8) {
                output[outputIndex++] = (buffer shr (bitsInBuffer - 8)).toByte()
                bitsInBuffer -= 8
            }
        }

        return output
    }

    /**
     * Validates if a string is valid Base32
     */
    fun isValid(input: String): Boolean {
        val sanitized = input.replace("=", "").replace(" ", "").replace("-", "")
        return sanitized.all { DECODE_TABLE[it.code] != -1 }
    }
}
