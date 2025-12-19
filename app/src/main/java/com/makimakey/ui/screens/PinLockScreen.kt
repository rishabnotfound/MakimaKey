package com.makimakey.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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

val SECURITY_QUESTIONS = listOf(
    "What is your favorite food?",
    "What is your favorite fruit?",
    "What is your favorite animal?",
    "What is your favorite color?"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinLockScreen(
    appLockManager: AppLockManager,
    onUnlocked: () -> Unit,
    onSetupPin: (String, String, String) -> Unit,
    onForgotPin: () -> Unit = {},
    isSetup: Boolean = false
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var securityAnswer by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showConfirm by remember { mutableStateOf(false) }
    var showSecurityQuestion by remember { mutableStateOf(false) }
    var selectedQuestionIndex by remember { mutableStateOf(0) }
    var expandedDropdown by remember { mutableStateOf(false) }

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
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = when {
                    isSetup && showSecurityQuestion -> "Security Question"
                    isSetup && showConfirm -> "Confirm PIN"
                    isSetup -> "Set PIN"
                    else -> "Enter PIN"
                },
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isSetup && showSecurityQuestion) {
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = SECURITY_QUESTIONS[selectedQuestionIndex],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Question") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        SECURITY_QUESTIONS.forEachIndexed { index, question ->
                            DropdownMenuItem(
                                text = { Text(question) },
                                onClick = {
                                    selectedQuestionIndex = index
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                    when {
                        isSetup && showSecurityQuestion -> {
                            if (securityAnswer.isNotBlank()) {
                                onSetupPin(pin, SECURITY_QUESTIONS[selectedQuestionIndex], securityAnswer)
                            } else {
                                errorMessage = "Security answer is required"
                            }
                        }
                        isSetup && showConfirm -> {
                            if (pin == confirmPin) {
                                showSecurityQuestion = true
                            } else {
                                errorMessage = "PINs do not match"
                                confirmPin = ""
                            }
                        }
                        isSetup && !showConfirm -> {
                            if (pin.length >= 4) {
                                showConfirm = true
                            } else {
                                errorMessage = "PIN must be at least 4 digits"
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = when {
                    isSetup && showSecurityQuestion -> securityAnswer.isNotBlank()
                    isSetup && showConfirm -> confirmPin.length >= 4
                    isSetup -> pin.length >= 4
                    else -> pin.length >= 4
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Text(
                    when {
                        isSetup && showSecurityQuestion -> "Finish"
                        isSetup && showConfirm -> "Next"
                        isSetup -> "Next"
                        else -> "Unlock"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (!isSetup) {
                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onForgotPin,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Forgot PIN?")
                }
            }
        }
    }
}
