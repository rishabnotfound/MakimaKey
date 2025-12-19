package com.makimakey.qr

import android.net.Uri
import com.makimakey.crypto.Base32Decoder
import com.makimakey.crypto.TotpGenerator

/**
 * Parses otpauth:// URIs from QR codes
 * Format: otpauth://totp/[Issuer]:[Account]?secret=BASE32&issuer=Issuer&algorithm=SHA1&digits=6&period=30
 */
object OtpAuthParser {

    data class OtpAuthData(
        val issuer: String,
        val accountName: String,
        val secret: ByteArray,
        val algorithm: TotpGenerator.Algorithm,
        val digits: Int,
        val period: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as OtpAuthData

            if (issuer != other.issuer) return false
            if (accountName != other.accountName) return false
            if (!secret.contentEquals(other.secret)) return false
            if (algorithm != other.algorithm) return false
            if (digits != other.digits) return false
            if (period != other.period) return false

            return true
        }

        override fun hashCode(): Int {
            var result = issuer.hashCode()
            result = 31 * result + accountName.hashCode()
            result = 31 * result + secret.contentHashCode()
            result = 31 * result + algorithm.hashCode()
            result = 31 * result + digits
            result = 31 * result + period
            return result
        }
    }

    /**
     * Parses an otpauth:// URI
     * @param uri The otpauth:// URI string
     * @return Parsed OTP data
     * @throws IllegalArgumentException if URI is invalid
     */
    fun parse(uri: String): OtpAuthData {
        require(uri.startsWith("otpauth://")) { "URI must start with otpauth://" }

        val parsedUri = Uri.parse(uri)
        val type = parsedUri.host?.lowercase()

        require(type == "totp") { "Only TOTP is supported (got: $type)" }

        // Parse label: "Issuer:Account" or just "Account"
        val label = parsedUri.path?.removePrefix("/") ?: throw IllegalArgumentException("Missing label")
        val labelParts = label.split(":", limit = 2)

        val issuerFromLabel = if (labelParts.size == 2) labelParts[0] else ""
        val accountName = if (labelParts.size == 2) labelParts[1] else labelParts[0]

        require(accountName.isNotBlank()) { "Account name cannot be empty" }

        // Parse query parameters
        val secretBase32 = parsedUri.getQueryParameter("secret")
            ?: throw IllegalArgumentException("Missing secret parameter")

        val issuerFromParam = parsedUri.getQueryParameter("issuer") ?: ""
        val issuer = issuerFromParam.ifBlank { issuerFromLabel }

        val algorithmStr = parsedUri.getQueryParameter("algorithm") ?: "SHA1"
        val algorithm = try {
            TotpGenerator.Algorithm.fromString(algorithmStr)
        } catch (e: Exception) {
            TotpGenerator.Algorithm.SHA1
        }

        val digits = parsedUri.getQueryParameter("digits")?.toIntOrNull() ?: 6
        require(digits in 6..8) { "Digits must be 6 or 8" }

        val period = parsedUri.getQueryParameter("period")?.toIntOrNull() ?: 30
        require(period > 0) { "Period must be positive" }

        // Decode secret
        val secret = try {
            Base32Decoder.decode(secretBase32)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid Base32 secret: ${e.message}")
        }

        require(secret.isNotEmpty()) { "Secret cannot be empty" }

        return OtpAuthData(
            issuer = issuer,
            accountName = accountName,
            secret = secret,
            algorithm = algorithm,
            digits = digits,
            period = period
        )
    }

    /**
     * Validates if a string is a valid otpauth:// URI
     */
    fun isValid(uri: String): Boolean {
        return try {
            parse(uri)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generates an otpauth:// URI from components (for backup/export)
     */
    fun generate(
        issuer: String,
        accountName: String,
        secretBase32: String,
        algorithm: TotpGenerator.Algorithm = TotpGenerator.Algorithm.SHA1,
        digits: Int = 6,
        period: Int = 30
    ): String {
        val label = if (issuer.isNotBlank()) {
            "${Uri.encode(issuer)}:${Uri.encode(accountName)}"
        } else {
            Uri.encode(accountName)
        }

        val params = buildString {
            append("secret=$secretBase32")
            if (issuer.isNotBlank()) append("&issuer=${Uri.encode(issuer)}")
            if (algorithm != TotpGenerator.Algorithm.SHA1) append("&algorithm=${algorithm.value}")
            if (digits != 6) append("&digits=$digits")
            if (period != 30) append("&period=$period")
        }

        return "otpauth://totp/$label?$params"
    }
}
