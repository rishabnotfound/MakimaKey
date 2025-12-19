package com.makimakey.security

import android.content.Context
import com.makimakey.storage.SecureStorage
import java.security.MessageDigest

/**
 * Manages app lock with PIN authentication
 */
class AppLockManager(
    private val context: Context,
    val secureStorage: SecureStorage
) {
    private var isUnlocked = false
    private var lastUnlockTime = 0L

    companion object {
        private const val AUTO_LOCK_TIMEOUT_MS = 30_000L // 30 seconds
        private const val SALT = "MakimaKey_PIN_SALT_v1"
    }

    /**
     * Checks if the app is currently unlocked
     */
    fun isUnlocked(): Boolean {
        if (!secureStorage.isAppLockEnabled()) {
            return true
        }

        val timeSinceUnlock = System.currentTimeMillis() - lastUnlockTime
        if (timeSinceUnlock > AUTO_LOCK_TIMEOUT_MS) {
            isUnlocked = false
        }

        return isUnlocked
    }

    /**
     * Marks the app as unlocked
     */
    fun unlock() {
        isUnlocked = true
        lastUnlockTime = System.currentTimeMillis()
    }

    /**
     * Locks the app
     */
    fun lock() {
        isUnlocked = false
    }

    /**
     * Sets up PIN authentication with security question
     */
    fun setupPin(pin: String, securityQuestion: String, securityAnswer: String) {
        require(pin.length >= 4) { "PIN must be at least 4 digits" }
        require(pin.all { it.isDigit() }) { "PIN must contain only digits" }
        require(securityQuestion.isNotBlank()) { "Security question is required" }
        require(securityAnswer.isNotBlank()) { "Security answer is required" }

        val pinHash = hashPin(pin)
        val answerHash = hashAnswer(securityAnswer)

        secureStorage.setPinHash(pinHash)
        secureStorage.setSecurityQuestion(securityQuestion)
        secureStorage.setSecurityAnswerHash(answerHash)
        secureStorage.setAppLockEnabled(true)
    }

    /**
     * Verifies a PIN
     */
    fun verifyPin(pin: String): Boolean {
        val storedHash = secureStorage.getPinHash() ?: return false
        val inputHash = hashPin(pin)
        return storedHash == inputHash
    }

    /**
     * Checks if a PIN is set
     */
    fun isPinSet(): Boolean {
        return secureStorage.getPinHash() != null
    }

    /**
     * Removes PIN authentication
     */
    fun removePin() {
        secureStorage.setPinHash("")
        secureStorage.setAppLockEnabled(false)
    }

    /**
     * Hashes a PIN using SHA-256
     */
    private fun hashPin(pin: String): String {
        val saltedPin = "$SALT$pin"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(saltedPin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Hashes a security answer using SHA-256
     */
    private fun hashAnswer(answer: String): String {
        val normalizedAnswer = answer.trim().lowercase()
        val saltedAnswer = "${SALT}_ANSWER_$normalizedAnswer"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(saltedAnswer.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies a security answer
     */
    fun verifySecurityAnswer(answer: String): Boolean {
        val storedHash = secureStorage.getSecurityAnswerHash() ?: return false
        val inputHash = hashAnswer(answer)
        return storedHash == inputHash
    }

    /**
     * Resets PIN using security answer
     */
    fun resetPinWithSecurityAnswer(answer: String, newPin: String): Boolean {
        if (!verifySecurityAnswer(answer)) {
            return false
        }

        require(newPin.length >= 4) { "PIN must be at least 4 digits" }
        require(newPin.all { it.isDigit() }) { "PIN must contain only digits" }

        val pinHash = hashPin(newPin)
        secureStorage.setPinHash(pinHash)
        return true
    }

    /**
     * Resets unlock timer (call when app comes to foreground)
     */
    fun refreshUnlockTime() {
        if (isUnlocked) {
            lastUnlockTime = System.currentTimeMillis()
        }
    }
}
