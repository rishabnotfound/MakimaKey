package com.makimakey.domain.repository

import com.makimakey.crypto.Base32Decoder
import com.makimakey.crypto.EncryptionManager
import com.makimakey.crypto.TotpGenerator
import com.makimakey.domain.model.TotpAccount
import com.makimakey.qr.OtpAuthParser
import com.makimakey.storage.SecureStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing TOTP accounts
 * Coordinates between storage and cryptography
 */
class TotpRepository(
    private val secureStorage: SecureStorage,
    private val encryptionManager: EncryptionManager
) {
    private val _accounts = MutableStateFlow<List<TotpAccount>>(emptyList())
    val accounts: StateFlow<List<TotpAccount>> = _accounts.asStateFlow()

    /**
     * Loads accounts from storage
     */
    suspend fun loadAccounts() {
        _accounts.value = secureStorage.loadAccounts()
    }

    /**
     * Adds a new account from OTP Auth data
     */
    suspend fun addAccount(otpAuthData: OtpAuthParser.OtpAuthData): Result<TotpAccount> {
        return try {
            val encryptedSecret = encryptionManager.encrypt(otpAuthData.secret)

            val account = TotpAccount(
                issuer = otpAuthData.issuer,
                accountName = otpAuthData.accountName,
                encryptedSecret = encryptedSecret,
                algorithm = otpAuthData.algorithm,
                digits = otpAuthData.digits,
                period = otpAuthData.period,
                order = _accounts.value.size
            )

            secureStorage.addAccount(account)
            loadAccounts()
            Result.success(account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds account from manual entry
     */
    suspend fun addAccountManual(
        issuer: String,
        accountName: String,
        secretBase32: String,
        algorithm: TotpGenerator.Algorithm = TotpGenerator.Algorithm.SHA1,
        digits: Int = 6,
        period: Int = 30
    ): Result<TotpAccount> {
        return try {
            val secret = Base32Decoder.decode(secretBase32)
            val encryptedSecret = encryptionManager.encrypt(secret)

            val account = TotpAccount(
                issuer = issuer,
                accountName = accountName,
                encryptedSecret = encryptedSecret,
                algorithm = algorithm,
                digits = digits,
                period = period,
                order = _accounts.value.size
            )

            secureStorage.addAccount(account)
            loadAccounts()
            Result.success(account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Deletes an account
     */
    suspend fun deleteAccount(accountId: String) {
        secureStorage.deleteAccount(accountId)
        loadAccounts()
    }

    /**
     * Reorders accounts
     */
    suspend fun reorderAccounts(accountIds: List<String>) {
        secureStorage.reorderAccounts(accountIds)
        loadAccounts()
    }

    /**
     * Generates current TOTP code for an account
     */
    fun generateTotp(account: TotpAccount): String {
        val secret = encryptionManager.decrypt(account.encryptedSecret)
        return TotpGenerator.generateTotp(
            secret = secret,
            period = account.period,
            digits = account.digits,
            algorithm = account.algorithm
        )
    }

    /**
     * Gets remaining seconds for current TOTP period
     */
    fun getRemainingSeconds(account: TotpAccount): Int {
        return TotpGenerator.getRemainingSeconds(period = account.period)
    }

    /**
     * Gets progress within current period (0.0 to 1.0)
     */
    fun getProgress(account: TotpAccount): Float {
        return TotpGenerator.getProgress(period = account.period)
    }

    /**
     * Decrypts secret for backup
     */
    fun decryptSecret(account: TotpAccount): ByteArray {
        return encryptionManager.decrypt(account.encryptedSecret)
    }

    /**
     * Exports account as otpauth URI (for backup)
     */
    fun exportAccountUri(account: TotpAccount): String {
        val secret = decryptSecret(account)
        val secretBase32 = android.util.Base64.encodeToString(secret, android.util.Base64.NO_WRAP)

        return OtpAuthParser.generate(
            issuer = account.issuer,
            accountName = account.accountName,
            secretBase32 = secretBase32,
            algorithm = account.algorithm,
            digits = account.digits,
            period = account.period
        )
    }
}
