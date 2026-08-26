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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentKey: String?,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
    onTestKey: () -> Unit,
    testResultMessage: String?,
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

            OutlinedButton(
                onClick = onTestKey,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.test_key))
            }
            testResultMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.back))
            }
        }
    }
}