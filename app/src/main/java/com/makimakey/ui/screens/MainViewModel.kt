package com.makimakey.ui.screens

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.makimakey.crypto.EncryptionManager
import com.makimakey.crypto.TotpGenerator
import com.makimakey.domain.model.TotpAccount
import com.makimakey.domain.repository.TotpRepository
import com.makimakey.qr.OtpAuthParser
import com.makimakey.security.AppLockManager
import com.makimakey.storage.SecureStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage(application)
    private val encryptionManager = EncryptionManager()
    private val repository = TotpRepository(secureStorage, encryptionManager)
    val appLockManager = AppLockManager(application, secureStorage)

    val accounts: StateFlow<List<TotpAccount>> = repository.accounts

    private val _currentCodes = MutableStateFlow<Map<String, String>>(emptyMap())
    val currentCodes: StateFlow<Map<String, String>> = _currentCodes.asStateFlow()

    private val _remainingSeconds = MutableStateFlow<Map<String, Int>>(emptyMap())
    val remainingSeconds: StateFlow<Map<String, Int>> = _remainingSeconds.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var updateJob: Job? = null

    init {
        loadAccounts()
        startPeriodicUpdate()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.loadAccounts()
                updateAllCodes()
            } catch (e: Exception) {
                _error.value = "Failed to load accounts: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startPeriodicUpdate() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch {
            while (isActive) {
                updateAllCodes()
                delay(1000)
            }
        }
    }

    private fun updateAllCodes() {
        val codes = mutableMapOf<String, String>()
        val remaining = mutableMapOf<String, Int>()

        accounts.value.forEach { account ->
            try {
                codes[account.id] = repository.generateTotp(account)
                remaining[account.id] = repository.getRemainingSeconds(account)
            } catch (e: Exception) {
                codes[account.id] = "ERROR"
                remaining[account.id] = 0
            }
        }

        _currentCodes.value = codes
        _remainingSeconds.value = remaining
    }

    fun addAccountFromQr(qrContent: String) {
        viewModelScope.launch {
            try {
                val otpAuthData = OtpAuthParser.parse(qrContent)
                val result = repository.addAccount(otpAuthData)
                if (result.isFailure) {
                    _error.value = "Failed to add account: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "Invalid QR code: ${e.message}"
            }
        }
    }

    fun addAccountManual(
        issuer: String,
        accountName: String,
        secret: String,
        algorithm: TotpGenerator.Algorithm,
        digits: Int,
        period: Int
    ) {
        viewModelScope.launch {
            try {
                val result = repository.addAccountManual(
                    issuer = issuer,
                    accountName = accountName,
                    secretBase32 = secret,
                    algorithm = algorithm,
                    digits = digits,
                    period = period
                )
                if (result.isFailure) {
                    _error.value = "Failed to add account: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to add account: ${e.message}"
            }
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(accountId)
            } catch (e: Exception) {
                _error.value = "Failed to delete account: ${e.message}"
            }
        }
    }

    fun copyToClipboard(code: String) {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("TOTP Code", code)
        clipboard.setPrimaryClip(clip)

        viewModelScope.launch {
            delay(30000)
            if (clipboard.primaryClip?.getItemAt(0)?.text == code) {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getFilteredAccounts(): List<TotpAccount> {
        val query = _searchQuery.value.lowercase()
        if (query.isBlank()) return accounts.value

        return accounts.value.filter { account ->
            account.issuer.lowercase().contains(query) ||
                    account.accountName.lowercase().contains(query)
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
    }
}
