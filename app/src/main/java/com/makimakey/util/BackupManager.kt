package com.makimakey.util

import android.content.Context
import android.net.Uri
import com.makimakey.crypto.EncryptionManager
import com.makimakey.domain.model.TotpAccount
import com.makimakey.domain.repository.TotpRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles encrypted backup and restore of TOTP accounts
 */
class BackupManager(
    private val context: Context,
    private val repository: TotpRepository
) {
    companion object {
        private const val BACKUP_VERSION = 1
        private const val PBKDF2_ITERATIONS = 100000
        private const val KEY_SIZE = 256
    }

    /**
     * Creates an encrypted backup
     * @param password Password to encrypt the backup
     * @return JSON string containing encrypted backup
     */
    suspend fun createBackup(password: String): String = withContext(Dispatchers.IO) {
        val accounts = repository.accounts.value

        val backupData = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("timestamp", System.currentTimeMillis())
            put("accounts", JSONArray().apply {
                accounts.forEach { account ->
                    put(accountToJson(account))
                }
            })
        }

        val plaintext = backupData.toString()
        encryptBackup(plaintext, password)
    }

    /**
     * Restores from an encrypted backup
     * @param backupJson Encrypted backup JSON
     * @param password Password to decrypt the backup
     * @return Result with number of accounts restored
     */
    suspend fun restoreBackup(backupJson: String, password: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val plaintext = decryptBackup(backupJson, password)
            val backupData = JSONObject(plaintext)

            val version = backupData.getInt("version")
            if (version != BACKUP_VERSION) {
                return@withContext Result.failure(Exception("Unsupported backup version: $version"))
            }

            val accountsArray = backupData.getJSONArray("accounts")
            var restoredCount = 0

            for (i in 0 until accountsArray.length()) {
                try {
                    val accountJson = accountsArray.getJSONObject(i)
                    val account = jsonToAccount(accountJson)

                    repository.addAccount(
                        com.makimakey.qr.OtpAuthParser.OtpAuthData(
                            issuer = account.issuer,
                            accountName = account.accountName,
                            secret = repository.decryptSecret(account),
                            algorithm = account.algorithm,
                            digits = account.digits,
                            period = account.period
                        )
                    )
                    restoredCount++
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports backup to a file
     */
    suspend fun exportToFile(uri: Uri, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backup = createBackup(password)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(backup.toByteArray())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports backup from a file
     */
    suspend fun importFromFile(uri: Uri, password: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val backupJson = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: return@withContext Result.failure(Exception("Failed to read file"))

            restoreBackup(backupJson, password)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun accountToJson(account: TotpAccount): JSONObject {
        return JSONObject().apply {
            put("issuer", account.issuer)
            put("accountName", account.accountName)
            put("encryptedSecret", account.encryptedSecret)
            put("algorithm", account.algorithm.value)
            put("digits", account.digits)
            put("period", account.period)
        }
    }

    private fun jsonToAccount(json: JSONObject): TotpAccount {
        return TotpAccount(
            issuer = json.optString("issuer", ""),
            accountName = json.getString("accountName"),
            encryptedSecret = json.getString("encryptedSecret"),
            algorithm = com.makimakey.crypto.TotpGenerator.Algorithm.fromString(
                json.optString("algorithm", "SHA1")
            ),
            digits = json.optInt("digits", 6),
            period = json.optInt("period", 30)
        )
    }

    /**
     * Encrypts backup data with password-based encryption
     */
    private fun encryptBackup(plaintext: String, password: String): String {
        val salt = ByteArray(16).apply {
            java.security.SecureRandom().nextBytes(this)
        }

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray())

        val result = JSONObject().apply {
            put("salt", android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP))
            put("iv", android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP))
            put("data", android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP))
        }

        return result.toString()
    }

    /**
     * Decrypts backup data
     */
    private fun decryptBackup(encrypted: String, password: String): String {
        val json = JSONObject(encrypted)

        val salt = android.util.Base64.decode(json.getString("salt"), android.util.Base64.NO_WRAP)
        val iv = android.util.Base64.decode(json.getString("iv"), android.util.Base64.NO_WRAP)
        val ciphertext = android.util.Base64.decode(json.getString("data"), android.util.Base64.NO_WRAP)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext)
    }

    /**
     * Derives encryption key from password using PBKDF2
     */
    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE)
        val key = factory.generateSecret(spec)
        return SecretKeySpec(key.encoded, "AES")
    }
}
