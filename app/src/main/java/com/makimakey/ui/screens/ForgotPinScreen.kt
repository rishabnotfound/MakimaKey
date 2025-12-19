package com.makimakey.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.makimakey.security.AppLockManager
import com.makimakey.ui.theme.TrueBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPinScreen(
    appLockManager: AppLockManager,
    onBackClick: () -> Unit,
    onPinReset: () -> Unit
) {
    var securityAnswer by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPinReset by remember { mutableStateOf(false) }

    val securityQuestion = appLockManager.secureStorage.getSecurityQuestion() ?: "No security question set"

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = TrueBlack
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = { Text("Forgot PIN") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TrueBlack
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = if (showPinReset) "Reset PIN" else "Security Question",
                    style = MaterialTheme.typography.headlineMedium
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (!showPinReset) {
                    Text(
                        text = securityQuestion,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = securityAnswer,
                        onValueChange = {
                            securityAnswer = it
                            errorMessage = null
                        },
                        label = { Text("Your Answer") },
                        singleLine = true,
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                newPin = it
                                errorMessage = null
                            }
                        },
                        label = { Text("New PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = confirmNewPin,
                        onValueChange = {
                            if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                confirmNewPin = it
                                errorMessage = null
                            }
                        },
                        label = { Text("Confirm New PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = errorMessage != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )
                }

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
                        if (!showPinReset) {
                            if (appLockManager.verifySecurityAnswer(securityAnswer)) {
                                showPinReset = true
                                errorMessage = null
                            } else {
                                errorMessage = "Incorrect answer"
                                securityAnswer = ""
                            }
                        } else {
                            when {
                                newPin.length < 4 -> {
                                    errorMessage = "PIN must be at least 4 digits"
                                }
                                newPin != confirmNewPin -> {
                                    errorMessage = "PINs do not match"
                                    confirmNewPin = ""
                                }
                                else -> {
                                    val success = appLockManager.resetPinWithSecurityAnswer(securityAnswer, newPin)
                                    if (success) {
                                        onPinReset()
                                    } else {
                                        errorMessage = "Failed to reset PIN"
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = if (showPinReset) {
                        newPin.length >= 4 && confirmNewPin.length >= 4
                    } else {
                        securityAnswer.isNotBlank()
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Text(
                        if (showPinReset) "Reset PIN" else "Verify",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
