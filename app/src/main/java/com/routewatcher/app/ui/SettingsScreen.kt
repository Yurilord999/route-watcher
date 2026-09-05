package com.routewatcher.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.routewatcher.app.R
import com.routewatcher.app.network.errorMessageRes
import com.routewatcher.app.viewmodel.ApiKeyTestResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentKey: String?,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
    onTestKey: () -> Unit,
    testResult: ApiKeyTestResult?,
    onBack: () -> Unit,
) {
    var keyInput by remember { mutableStateOf(currentKey ?: "") }
    var showKey by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
        ) {
            // ---- setup guide ----
            CollapsibleApiKeyGuide(initiallyExpanded = currentKey.isNullOrBlank())
            Spacer(Modifier.height(20.dp))

            // ---- API key input ----
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text(stringResource(R.string.api_key_label)) },
                singleLine = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { showKey = !showKey }) {
                Text(stringResource(if (showKey) R.string.hide_key else R.string.show_key))
            }
            Spacer(Modifier.height(24.dp))

            // ---- save / clear ----
            Row {
                Button(
                    onClick = { onSaveKey(keyInput) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.save_key))
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = { keyInput = ""; onClearKey() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.clear_key))
                }
            }
            Spacer(Modifier.height(24.dp))

            // ---- test key ----
            OutlinedButton(
                onClick = onTestKey,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.test_key))
            }
            Spacer(Modifier.height(8.dp))

            testResult?.let { result ->
                Spacer(Modifier.height(8.dp))
                val message = if (result.success) {
                    stringResource(R.string.test_key_success, result.durationMinutes)
                } else {
                    stringResource(R.string.test_key_failed, stringResource(errorMessageRes(result.errorCode)))
                }
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))

            // ---- navigation ----
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.back))
            }
        }
    }
}