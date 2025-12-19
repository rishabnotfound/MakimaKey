package com.makimakey.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.makimakey.crypto.Base32Decoder
import com.makimakey.crypto.TotpGenerator
import com.makimakey.ui.theme.TrueBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountScreen(
    onAddAccount: (String, String, String, TotpGenerator.Algorithm, Int, Int) -> Unit,
    onBackClick: () -> Unit
) {
    var issuer by remember { mutableStateOf("") }
    var accountName by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var algorithm by remember { mutableStateOf(TotpGenerator.Algorithm.SHA1) }
    var digits by remember { mutableStateOf(6) }
    var period by remember { mutableStateOf(30) }

    var showAdvanced by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Account") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = issuer,
                onValueChange = { issuer = it },
                label = { Text("Issuer (Optional)") },
                placeholder = { Text("Google, GitHub, etc.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text("Account Name") },
                placeholder = { Text("user@example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = accountName.isBlank() && errorMessage != null
            )

            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it.uppercase().replace(" ", "").replace("-", "") },
                label = { Text("Secret Key") },
                placeholder = { Text("BASE32 ENCODED SECRET") },
                modifier = Modifier.fillMaxWidth(),
                isError = !Base32Decoder.isValid(secret) && secret.isNotBlank()
            )

            if (!Base32Decoder.isValid(secret) && secret.isNotBlank()) {
                Text(
                    text = "Invalid Base32 secret",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) "Hide Advanced Options" else "Show Advanced Options")
            }

            if (showAdvanced) {
                Text(
                    text = "Algorithm",
                    style = MaterialTheme.typography.titleSmall
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = algorithm == TotpGenerator.Algorithm.SHA1,
                        onClick = { algorithm = TotpGenerator.Algorithm.SHA1 },
                        label = { Text("SHA1") }
                    )
                    FilterChip(
                        selected = algorithm == TotpGenerator.Algorithm.SHA256,
                        onClick = { algorithm = TotpGenerator.Algorithm.SHA256 },
                        label = { Text("SHA256") }
                    )
                    FilterChip(
                        selected = algorithm == TotpGenerator.Algorithm.SHA512,
                        onClick = { algorithm = TotpGenerator.Algorithm.SHA512 },
                        label = { Text("SHA512") }
                    )
                }

                OutlinedTextField(
                    value = digits.toString(),
                    onValueChange = { if (it.toIntOrNull() in 6..8) digits = it.toInt() },
                    label = { Text("Digits") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = period.toString(),
                    onValueChange = { it.toIntOrNull()?.let { p -> if (p > 0) period = p } },
                    label = { Text("Period (seconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            errorMessage?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    when {
                        accountName.isBlank() -> errorMessage = "Account name is required"
                        secret.isBlank() -> errorMessage = "Secret key is required"
                        !Base32Decoder.isValid(secret) -> errorMessage = "Invalid Base32 secret"
                        else -> {
                            onAddAccount(issuer, accountName, secret, algorithm, digits, period)
                            onBackClick()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Account")
            }
        }
    }
}
