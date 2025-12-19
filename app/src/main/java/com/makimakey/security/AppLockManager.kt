package com.makimakey.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.makimakey.storage.SecureStorage
import java.security.MessageDigest

/**
 * Manages app lock with PIN and biometric authentication
 */
class AppLockManager(
    private val context: Context,
    private val secureStorage: SecureStorage
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
     * Sets up PIN authentication
     */
    fun setupPin(pin: String) {
        require(pin.length >= 4) { "PIN must be at least 4 digits" }
        require(pin.all { it.isDigit() }) { "PIN must contain only digits" }

        val hash = hashPin(pin)
        secureStorage.setPinHash(hash)
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
        secureStorage.setBiometricEnabled(false)
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
     * Checks if biometric authentication is available
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    /**
     * Checks if biometric is enabled
     */
    fun isBiometricEnabled(): Boolean {
        return secureStorage.isBiometricEnabled() && isBiometricAvailable()
    }

    /**
     * Enables biometric authentication
     */
    fun enableBiometric() {
        require(isBiometricAvailable()) { "Biometric authentication not available" }
        require(isPinSet()) { "PIN must be set before enabling biometric" }
        secureStorage.setBiometricEnabled(true)
    }

    /**
     * Disables biometric authentication
     */
    fun disableBiometric() {
        secureStorage.setBiometricEnabled(false)
    }

    /**
     * Shows biometric prompt
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    unlock()
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock MakimaKey")
            .setSubtitle("Authenticate to access your accounts")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
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
