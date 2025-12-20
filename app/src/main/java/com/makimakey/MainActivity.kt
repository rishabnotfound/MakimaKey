package com.makimakey

import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.makimakey.ui.screens.*
import com.makimakey.ui.theme.MakimaKeyTheme
import com.makimakey.ui.theme.TrueBlack
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()

        setContent {
            MakimaKeyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = TrueBlack
                ) {
                    MakimaKeyApp()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }
}

@Composable
fun MakimaKeyApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val accounts by viewModel.accounts.collectAsState()
    val currentCodes by viewModel.currentCodes.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val error by viewModel.error.collectAsState()

    var isUnlocked by remember { mutableStateOf(!viewModel.appLockManager.isPinSet()) }
    var showPinSetup by remember { mutableStateOf(false) }
    var showForgotPin by remember { mutableStateOf(false) }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val success = viewModel.exportBackup(it)
                if (success) {
                    Toast.makeText(
                        navController.context,
                        "Backup exported successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                viewModel.importBackup(it)
            }
        }
    }

    error?.let { errorMsg ->
        LaunchedEffect(errorMsg) {
            Toast.makeText(
                navController.context,
                errorMsg,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.clearError()
        }
    }

    if (showForgotPin) {
        ForgotPinScreen(
            appLockManager = viewModel.appLockManager,
            onBackClick = { showForgotPin = false },
            onPinReset = {
                Toast.makeText(
                    navController.context,
                    "PIN reset successfully",
                    Toast.LENGTH_SHORT
                ).show()
                showForgotPin = false
                isUnlocked = true
            }
        )
    } else if (!isUnlocked && viewModel.appLockManager.isPinSet()) {
        PinLockScreen(
            appLockManager = viewModel.appLockManager,
            onUnlocked = { isUnlocked = true },
            onSetupPin = { _, _, _ -> },
            onForgotPin = { showForgotPin = true },
            isSetup = false
        )
    } else if (showPinSetup) {
        PinLockScreen(
            appLockManager = viewModel.appLockManager,
            onUnlocked = { showPinSetup = false },
            onSetupPin = { pin, securityQuestion, securityAnswer ->
                viewModel.appLockManager.setupPin(pin, securityQuestion, securityAnswer)
                showPinSetup = false
            },
            isSetup = true
        )
    } else {
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                HomeScreen(
                    accounts = viewModel.getFilteredAccounts(),
                    currentCodes = currentCodes,
                    remainingSeconds = remainingSeconds,
                    searchQuery = searchQuery,
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onAccountClick = { account, code ->
                        viewModel.copyToClipboard(code)
                        Toast.makeText(
                            navController.context,
                            "Copied to clipboard",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onAccountEdit = { account, issuer, accountName ->
                        viewModel.updateAccountDetails(account.id, issuer, accountName)
                    },
                    onAddClick = {
                        navController.navigate("add")
                    },
                    onQrScanClick = {
                        navController.navigate("qr_scan")
                    },
                    onSettingsClick = {
                        navController.navigate("settings")
                    },
                    onDeleteAccount = viewModel::deleteAccount
                )
            }

            composable("add") {
                AddAccountScreen(
                    onAddAccount = { issuer, accountName, secret, algorithm, digits, period ->
                        viewModel.addAccountManual(issuer, accountName, secret, algorithm, digits, period)
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable("qr_scan") {
                val scope = rememberCoroutineScope()
                var isProcessing by remember { mutableStateOf(false) }

                QrScannerScreen(
                    onQrScanned = { qrContent ->
                        if (!isProcessing) {
                            isProcessing = true
                            scope.launch {
                                val success = viewModel.addAccountFromQr(qrContent)
                                if (success) {
                                    Toast.makeText(
                                        navController.context,
                                        "Account added successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack()
                                } else {
                                    isProcessing = false
                                }
                            }
                        }
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    appLockManager = viewModel.appLockManager,
                    onBackClick = { navController.popBackStack() },
                    onSetupPinClick = {
                        showPinSetup = true
                    },
                    onExportBackup = {
                        exportBackupLauncher.launch(viewModel.backupManager.generateBackupFileName())
                    },
                    onImportBackup = {
                        importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                    }
                )
            }
        }
    }
}
