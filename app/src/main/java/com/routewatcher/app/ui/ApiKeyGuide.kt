package com.routewatcher.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import com.routewatcher.app.R

private const val CONSOLE_URL = "https://console.cloud.google.com"

// ---- API key guide (shared by settings and onboarding screen) ----
@Composable
fun ApiKeyGuideSteps(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GuideStep(1) {
            Column {
                Text(stringResource(R.string.guide_step_1), style = MaterialTheme.typography.bodySmall)
                Text(
                    "console.cloud.google.com",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { uriHandler.openUri(CONSOLE_URL) },
                )
            }
        }
        GuideStep(2) { Text(stringResource(R.string.guide_step_2), style = MaterialTheme.typography.bodySmall) }
        GuideStep(3) { Text(stringResource(R.string.guide_step_3), style = MaterialTheme.typography.bodySmall) }
        GuideStep(4) { Text(stringResource(R.string.guide_step_4), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun GuideStep(number: Int, content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        content()
    }
}

// ---- collapsible wrapper (API guide settings)----
@Composable
fun CollapsibleApiKeyGuide(initiallyExpanded: Boolean, modifier: Modifier = Modifier) {
    var expanded by remember(initiallyExpanded) { mutableStateOf(initiallyExpanded) }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.guide_toggle_label),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(if (expanded) "\u25BE" else "\u25B8", style = MaterialTheme.typography.bodyMedium)
        }
        if (expanded) {
            Spacer(Modifier.height(14.dp))
            ApiKeyGuideSteps()
        }
    }
}