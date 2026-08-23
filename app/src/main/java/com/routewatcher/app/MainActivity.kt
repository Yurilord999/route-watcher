package com.routewatcher.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.routewatcher.app.ui.AddEditRouteScreen
import com.routewatcher.app.ui.RouteListScreen
import com.routewatcher.app.ui.SettingsScreen
import com.routewatcher.app.ui.theme.RouteWatcherTheme

private sealed class Screen {
    data object List : Screen()
    data object AddEdit : Screen()
    data object Settings : Screen()
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RouteWatcherTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.List) }

                when (screen) {
                    is Screen.List -> RouteListScreen(
                        onAddRoute = { screen = Screen.AddEdit },
                        onOpenSettings = { screen = Screen.Settings },
                    )
                    is Screen.AddEdit -> AddEditRouteScreen(
                        onSave = { screen = Screen.List },
                        onCancel = { screen = Screen.List },
                    )
                    is Screen.Settings -> SettingsScreen(
                        onBack = { screen = Screen.List },
                    )
                }
            }
        }
    }
}
