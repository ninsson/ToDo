package com.example.todo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Settings") })
    }) { padding ->
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Text("Settings (placeholders)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Notification preferences, location settings, backup", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }
    }
}