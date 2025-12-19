package com.makimakey.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Handles encryption/decryption using Android Keystore with AES-GCM
 * Keys are stored securely in hardware-backed Android Keystore when available
 */
class EncryptionManager {
    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "MakimaKey_Master"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_SIZE = 12

        private const val IV_SEPARATOR = ":"
    }

    init {
        ensureKeyExists()
    }

    /**
     * Ensures the master encryption key exists in the Keystore
     * Creates a new key if it doesn't exist
     */
    private fun ensureKeyExists() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            createKey()
        }
    }

    /**
     * Creates a new AES-256 key in Android Keystore
     */
    private fun createKey() {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        keyGenerator.generateKey()
    }

    /**
     * Retrieves the master key from Keystore
     */
    private fun getKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * Encrypts data using AES-GCM
     * @param plaintext Data to encrypt
     * @return Encrypted data with IV prepended, separated by ':'
     */
    fun encrypt(plaintext: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)

        // Combine IV and ciphertext: "IV:ciphertext" in Base64
        val ivBase64 = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP)
        val ciphertextBase64 = android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP)

        return "$ivBase64$IV_SEPARATOR$ciphertextBase64"
    }

    /**
     * Encrypts a string
     */
    fun encrypt(plaintext: String): String {
        return encrypt(plaintext.toByteArray(Charsets.UTF_8))
    }

    /**
     * Decrypts data encrypted with encrypt()
     * @param encrypted Encrypted data string (IV:ciphertext format)
     * @return Decrypted byte array
     */
    fun decrypt(encrypted: String): ByteArray {
        val parts = encrypted.split(IV_SEPARATOR)
        require(parts.size == 2) { "Invalid encrypted data format" }

        val iv = android.util.Base64.decode(parts[0], android.util.Base64.NO_WRAP)
        val ciphertext = android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)

        return cipher.doFinal(ciphertext)
    }

    /**
     * Decrypts to a string
     */
    fun decryptToString(encrypted: String): String {
        return String(decrypt(encrypted), Charsets.UTF_8)
    }

    /**
     * Checks if the master key exists
     */
    fun keyExists(): Boolean {
        return keyStore.containsAlias(KEY_ALIAS)
    }

    /**
     * Deletes the master key (use with caution - will make all encrypted data unrecoverable)
     */
    fun deleteKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }
}
