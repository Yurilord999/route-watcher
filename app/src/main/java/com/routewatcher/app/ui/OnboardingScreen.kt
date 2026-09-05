package com.routewatcher.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.routewatcher.app.R

// Shown once on first launch
@Composable
fun OnboardingScreen(
    onSaveKey: (String) -> Unit,
    onFinished: () -> Unit,
) {
    var keyInput by remember { mutableStateOf("") }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // ---- welcome header ----
            Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            // ---- setup guide ----
            ApiKeyGuideSteps()
            Spacer(Modifier.height(24.dp))

            // ---- API key input ----
            ApiKeyInputField(value = keyInput, onValueChange = { keyInput = it })
            Spacer(Modifier.height(20.dp))

            // ---- actions ----
            Button(
                onClick = {
                    onSaveKey(keyInput)
                    onFinished()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save_and_continue))
            }
            TextButton(
                onClick = onFinished,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.skip_for_now))
            }
        }
    }
}