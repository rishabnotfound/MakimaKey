package com.makimakey

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

    val accounts by viewModel.accounts.collectAsState()
    val currentCodes by viewModel.currentCodes.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val error by viewModel.error.collectAsState()

    var isUnlocked by remember { mutableStateOf(!viewModel.appLockManager.isPinSet()) }
    var showPinSetup by remember { mutableStateOf(false) }

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

    if (!isUnlocked && viewModel.appLockManager.isPinSet()) {
        PinLockScreen(
            appLockManager = viewModel.appLockManager,
            onUnlocked = { isUnlocked = true },
            onSetupPin = {},
            isSetup = false
        )
    } else if (showPinSetup) {
        PinLockScreen(
            appLockManager = viewModel.appLockManager,
            onUnlocked = { showPinSetup = false },
            onSetupPin = { pin ->
                viewModel.appLockManager.setupPin(pin)
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
                QrScannerScreen(
                    onQrScanned = { qrContent ->
                        viewModel.addAccountFromQr(qrContent)
                        navController.popBackStack()
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
                    }
                )
            }
        }
    }
}
