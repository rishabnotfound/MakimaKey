package com.makimakey.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.makimakey.crypto.EncryptionManager
import com.makimakey.crypto.TotpGenerator
import com.makimakey.domain.model.TotpAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Secure storage for TOTP accounts using EncryptedSharedPreferences
 * All secrets are encrypted with AES-GCM before storage
 */
class SecureStorage(context: Context) {
    private val encryptionManager = EncryptionManager()

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val PREFS_NAME = "makimakey_secure_prefs"
        private const val KEY_ACCOUNTS = "accounts"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_SECURITY_QUESTION = "security_question"
        private const val KEY_SECURITY_ANSWER_HASH = "security_answer_hash"
    }

    /**
     * Saves all accounts to secure storage
     */
    suspend fun saveAccounts(accounts: List<TotpAccount>) = withContext(Dispatchers.IO) {
        val jsonArray = JSONArray()
        accounts.forEach { account ->
            jsonArray.put(accountToJson(account))
        }
        sharedPreferences.edit()
            .putString(KEY_ACCOUNTS, jsonArray.toString())
            .apply()
    }

    /**
     * Loads all accounts from secure storage
     */
    suspend fun loadAccounts(): List<TotpAccount> = withContext(Dispatchers.IO) {
        val jsonString = sharedPreferences.getString(KEY_ACCOUNTS, null) ?: return@withContext emptyList()
        val jsonArray = JSONArray(jsonString)
        val accounts = mutableListOf<TotpAccount>()

        for (i in 0 until jsonArray.length()) {
            try {
                accounts.add(jsonFromAccount(jsonArray.getJSONObject(i)))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        accounts.sortedBy { it.order }
    }

    /**
     * Adds a new account
     */
    suspend fun addAccount(account: TotpAccount) {
        val accounts = loadAccounts().toMutableList()
        accounts.add(account)
        saveAccounts(accounts)
    }

    /**
     * Updates an existing account
     */
    suspend fun updateAccount(account: TotpAccount) {
        val accounts = loadAccounts().toMutableList()
        val index = accounts.indexOfFirst { it.id == account.id }
        if (index != -1) {
            accounts[index] = account
            saveAccounts(accounts)
        }
    }

    /**
     * Deletes an account
     */
    suspend fun deleteAccount(accountId: String) {
        val accounts = loadAccounts().toMutableList()
        accounts.removeIf { it.id == accountId }
        saveAccounts(accounts)
    }

    /**
     * Reorders accounts
     */
    suspend fun reorderAccounts(accountIds: List<String>) {
        val accounts = loadAccounts().associateBy { it.id }
        val reordered = accountIds.mapIndexedNotNull { index, id ->
            accounts[id]?.withOrder(index)
        }
        saveAccounts(reordered)
    }

    /**
     * Converts account to JSON
     */
    private fun accountToJson(account: TotpAccount): JSONObject {
        return JSONObject().apply {
            put("id", account.id)
            put("issuer", account.issuer)
            put("accountName", account.accountName)
            put("encryptedSecret", account.encryptedSecret)
            put("algorithm", account.algorithm.value)
            put("digits", account.digits)
            put("period", account.period)
            put("order", account.order)
            put("createdAt", account.createdAt)
        }
    }

    /**
     * Converts JSON to account
     */
    private fun jsonFromAccount(json: JSONObject): TotpAccount {
        return TotpAccount(
            id = json.getString("id"),
            issuer = json.optString("issuer", ""),
            accountName = json.getString("accountName"),
            encryptedSecret = json.getString("encryptedSecret"),
            algorithm = TotpGenerator.Algorithm.fromString(json.optString("algorithm", "SHA1")),
            digits = json.optInt("digits", 6),
            period = json.optInt("period", 30),
            order = json.optInt("order", 0),
            createdAt = json.optLong("createdAt", System.currentTimeMillis())
        )
    }

    /**
     * App lock settings
     */
    fun isAppLockEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_APP_LOCK_ENABLED, false)
    }

    fun setAppLockEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, enabled)
            .apply()
    }

    fun getPinHash(): String? {
        return sharedPreferences.getString(KEY_PIN_HASH, null)
    }

    fun setPinHash(hash: String) {
        sharedPreferences.edit()
            .putString(KEY_PIN_HASH, hash)
            .apply()
    }

    fun getSecurityQuestion(): String? {
        return sharedPreferences.getString(KEY_SECURITY_QUESTION, null)
    }

    fun setSecurityQuestion(question: String) {
        sharedPreferences.edit()
            .putString(KEY_SECURITY_QUESTION, question)
            .apply()
    }

    fun getSecurityAnswerHash(): String? {
        return sharedPreferences.getString(KEY_SECURITY_ANSWER_HASH, null)
    }

    fun setSecurityAnswerHash(hash: String) {
        sharedPreferences.edit()
            .putString(KEY_SECURITY_ANSWER_HASH, hash)
            .apply()
    }

    /**
     * Clears all data (for security wipe)
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}
