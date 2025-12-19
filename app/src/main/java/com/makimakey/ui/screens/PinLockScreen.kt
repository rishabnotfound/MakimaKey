package com.makimakey.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.makimakey.security.AppLockManager
import com.makimakey.ui.theme.TrueBlack

@Composable
fun PinLockScreen(
    appLockManager: AppLockManager,
    onUnlocked: () -> Unit,
    onSetupPin: (String) -> Unit,
    isSetup: Boolean = false
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = TrueBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (isSetup) {
                    if (showConfirm) "Confirm PIN" else "Set PIN"
                } else {
                    "Enter PIN"
                },
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = if (showConfirm) confirmPin else pin,
                onValueChange = {
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        if (showConfirm) {
                            confirmPin = it
                        } else {
                            pin = it
                        }
                        errorMessage = null
                    }
                },
                label = { Text("PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = errorMessage != null,
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    when {
                        isSetup && !showConfirm -> {
                            if (pin.length >= 4) {
                                showConfirm = true
                            } else {
                                errorMessage = "PIN must be at least 4 digits"
                            }
                        }
                        isSetup && showConfirm -> {
                            if (pin == confirmPin) {
                                onSetupPin(pin)
                            } else {
                                errorMessage = "PINs do not match"
                                confirmPin = ""
                            }
                        }
                        else -> {
                            if (appLockManager.verifyPin(pin)) {
                                appLockManager.unlock()
                                onUnlocked()
                            } else {
                                errorMessage = "Incorrect PIN"
                                pin = ""
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = (if (showConfirm) confirmPin else pin).length >= 4
            ) {
                Text(
                    if (isSetup) {
                        if (showConfirm) "Confirm" else "Next"
                    } else {
                        "Unlock"
                    }
                )
            }

            if (!isSetup && appLockManager.isBiometricEnabled()) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        /* Biometric prompt would be shown here */
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Biometric")
                }
            }
        }
    }
}
