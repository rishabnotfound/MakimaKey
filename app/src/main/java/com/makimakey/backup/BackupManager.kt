package com.makimakey.backup

import android.content.Context
import android.net.Uri
import com.makimakey.storage.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages encrypted backups of TOTP accounts
 */
class BackupManager(
    private val context: Context,
    private val secureStorage: SecureStorage
) {

    /**
     * Exports all accounts to a backup file
     */
    suspend fun exportBackup(outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val accounts = secureStorage.loadAccounts()
            val backupJson = JSONObject().apply {
                put("version", 1)
                put("exported_at", System.currentTimeMillis())
                put("app", "MakimaKey")

                val accountsArray = JSONArray()
                accounts.forEach { account ->
                    accountsArray.put(JSONObject().apply {
                        put("id", account.id)
                        put("issuer", account.issuer)
                        put("accountName", account.accountName)
                        put("encryptedSecret", account.encryptedSecret)
                        put("algorithm", account.algorithm.value)
                        put("digits", account.digits)
                        put("period", account.period)
                        put("order", account.order)
                        put("createdAt", account.createdAt)
                    })
                }
                put("accounts", accountsArray)
            }

            outputStream.use {
                it.write(backupJson.toString().toByteArray())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Imports accounts from a backup file
     */
    suspend fun importBackup(inputStream: InputStream): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val backupContent = inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            val backupJson = JSONObject(backupContent)

            // Validate backup
            if (backupJson.optString("app") != "MakimaKey") {
                return@withContext Result.failure(Exception("Invalid backup file"))
            }

            val accountsArray = backupJson.getJSONArray("accounts")
            val currentAccounts = secureStorage.loadAccounts().toMutableList()
            val existingIds = currentAccounts.map { it.id }.toSet()

            var importedCount = 0

            for (i in 0 until accountsArray.length()) {
                val accountJson = accountsArray.getJSONObject(i)
                val accountId = accountJson.getString("id")

                // Skip if account already exists
                if (existingIds.contains(accountId)) {
                    continue
                }

                currentAccounts.add(
                    com.makimakey.domain.model.TotpAccount(
                        id = accountId,
                        issuer = accountJson.optString("issuer", ""),
                        accountName = accountJson.getString("accountName"),
                        encryptedSecret = accountJson.getString("encryptedSecret"),
                        algorithm = com.makimakey.crypto.TotpGenerator.Algorithm.fromString(
                            accountJson.optString("algorithm", "SHA1")
                        ),
                        digits = accountJson.optInt("digits", 6),
                        period = accountJson.optInt("period", 30),
                        order = accountJson.optInt("order", 0),
                        createdAt = accountJson.optLong("createdAt", System.currentTimeMillis())
                    )
                )
                importedCount++
            }

            if (importedCount > 0) {
                secureStorage.saveAccounts(currentAccounts)
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a default backup filename
     */
    fun generateBackupFileName(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "makimakey_backup_$timestamp.json"
    }
}
