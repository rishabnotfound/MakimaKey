package com.makimakey.domain.model

import com.makimakey.crypto.TotpGenerator
import java.util.UUID

/**
 * Represents a TOTP account with encrypted secret
 */
data class TotpAccount(
    val id: String = UUID.randomUUID().toString(),
    val issuer: String,
    val accountName: String,
    val encryptedSecret: String,
    val algorithm: TotpGenerator.Algorithm = TotpGenerator.Algorithm.SHA1,
    val digits: Int = 6,
    val period: Int = 30,
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Gets display name in format "Issuer (Account)" or just "Account" if no issuer
     */
    fun getDisplayName(): String {
        return if (issuer.isNotBlank()) {
            "$issuer ($accountName)"
        } else {
            accountName
        }
    }

    /**
     * Gets a short display name for space-constrained UI
     */
    fun getShortName(): String {
        return if (issuer.isNotBlank()) issuer else accountName
    }

    /**
     * Creates a copy with updated order
     */
    fun withOrder(newOrder: Int): TotpAccount {
        return copy(order = newOrder)
    }
}
