package com.makimakey.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.makimakey.security.AppLockManager
import com.makimakey.ui.theme.TrueBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appLockManager: AppLockManager,
    onBackClick: () -> Unit,
    onSetupPinClick: () -> Unit
) {
    val isPinSet = remember { appLockManager.isPinSet() }
    val isBiometricAvailable = remember { appLockManager.isBiometricAvailable() }
    var isBiometricEnabled by remember { mutableStateOf(appLockManager.isBiometricEnabled()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TrueBlack
                )
            )
        },
        containerColor = TrueBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Security",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            SettingsItem(
                title = if (isPinSet) "Change PIN" else "Set PIN",
                subtitle = if (isPinSet) "Modify your app PIN" else "Secure your app with a PIN",
                onClick = onSetupPinClick
            )

            if (isPinSet && isBiometricAvailable) {
                SwitchSettingsItem(
                    title = "Biometric Unlock",
                    subtitle = "Use fingerprint or face unlock",
                    checked = isBiometricEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            appLockManager.enableBiometric()
                        } else {
                            appLockManager.disableBiometric()
                        }
                        isBiometricEnabled = enabled
                    }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            SettingsItem(
                title = "Version",
                subtitle = "1.0.0"
            )

            SettingsItem(
                title = "MakimaKey",
                subtitle = "Secure offline TOTP authenticator"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This app is fully offline and stores all data locally. " +
                        "No data is ever transmitted over the network.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        modifier = Modifier.fillMaxWidth(),
        color = TrueBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun SwitchSettingsItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
