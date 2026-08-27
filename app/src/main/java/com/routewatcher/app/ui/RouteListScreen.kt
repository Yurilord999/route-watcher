package com.routewatcher.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.routewatcher.app.R
import com.routewatcher.app.data.RouteEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListScreen(
    routes: List<RouteEntity>,
    onAddRoute: () -> Unit,
    onEditRoute: (RouteEntity) -> Unit,
    onToggleRoute: (RouteEntity, Boolean) -> Unit,
    onOpenSettings: () -> Unit
){
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name))},
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRoute) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_route),
                )
            }
        }
    ) { padding ->
        if (routes.isEmpty()) {
            Box(
                modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.no_routes_yet))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
            ) {
                items(routes, key = { it.id }) { route ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onEditRoute(route) },
                        ) {
                            Text(route.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${route.originAddress} -> ${route.destinationAddress}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = route.enabled,
                            onCheckedChange = { onToggleRoute(route, it) },
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}