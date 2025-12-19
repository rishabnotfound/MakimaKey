package com.makimakey.crypto

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * RFC 6238 compliant TOTP generator
 * Implements Time-Based One-Time Password Algorithm with proper dynamic truncation
 */
object TotpGenerator {

    enum class Algorithm(val value: String, val macAlgorithm: String) {
        SHA1("SHA1", "HmacSHA1"),
        SHA256("SHA256", "HmacSHA256"),
        SHA512("SHA512", "HmacSHA512");

        companion object {
            fun fromString(value: String): Algorithm {
                return when (value.uppercase()) {
                    "SHA1", "SHA-1" -> SHA1
                    "SHA256", "SHA-256" -> SHA256
                    "SHA512", "SHA-512" -> SHA512
                    else -> SHA1
                }
            }
        }
    }

    /**
     * Generates a TOTP code
     * @param secret The shared secret key (decoded from Base32)
     * @param timeSeconds Current Unix time in seconds
     * @param period Time step period in seconds (usually 30)
     * @param digits Number of digits in the OTP (6 or 8)
     * @param algorithm Hash algorithm (SHA1, SHA256, SHA512)
     * @return The TOTP code as a zero-padded string
     */
    fun generateTotp(
        secret: ByteArray,
        timeSeconds: Long = System.currentTimeMillis() / 1000,
        period: Int = 30,
        digits: Int = 6,
        algorithm: Algorithm = Algorithm.SHA1
    ): String {
        require(secret.isNotEmpty()) { "Secret cannot be empty" }
        require(period > 0) { "Period must be positive" }
        require(digits in 6..8) { "Digits must be 6 or 8" }

        // Calculate time counter (T)
        val counter = timeSeconds / period

        // Generate HOTP
        val code = generateHotp(secret, counter, digits, algorithm)

        return code
    }

    /**
     * Generates an HOTP code (RFC 4226)
     * Used internally by TOTP
     */
    private fun generateHotp(
        secret: ByteArray,
        counter: Long,
        digits: Int,
        algorithm: Algorithm
    ): String {
        // Convert counter to 8-byte array (big-endian)
        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()

        // HMAC-SHA calculation
        val mac = Mac.getInstance(algorithm.macAlgorithm)
        val keySpec = SecretKeySpec(secret, algorithm.macAlgorithm)
        mac.init(keySpec)
        val hash = mac.doFinal(counterBytes)

        // Dynamic truncation (RFC 4226 Section 5.3)
        val offset = (hash.last().toInt() and 0x0F)
        val truncatedHash = (
            ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        )

        // Generate OTP code
        val divisor = 10.0.pow(digits).toInt()
        val otp = truncatedHash % divisor

        // Zero-pad to required digits
        return otp.toString().padStart(digits, '0')
    }

    /**
     * Calculates remaining time in current period
     * @param timeSeconds Current Unix time in seconds
     * @param period Time step period in seconds
     * @return Remaining seconds in current period
     */
    fun getRemainingSeconds(
        timeSeconds: Long = System.currentTimeMillis() / 1000,
        period: Int = 30
    ): Int {
        return (period - (timeSeconds % period)).toInt()
    }

    /**
     * Calculates progress within current period (0.0 to 1.0)
     * @param timeSeconds Current Unix time in seconds
     * @param period Time step period in seconds
     * @return Progress as a fraction (0.0 = start, 1.0 = end)
     */
    fun getProgress(
        timeSeconds: Long = System.currentTimeMillis() / 1000,
        period: Int = 30
    ): Float {
        val elapsed = (timeSeconds % period).toFloat()
        return elapsed / period
    }
}
