package com.example.todo.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ustawienia") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        // Używamy Icons.Default zamiast AutoMirrored
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Wstecz"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Powiadomienia",
                subtitle = "Zarządzaj alertami i dźwiękiem"
            )
            SettingsItem(
                icon = Icons.Default.LocationOn,
                title = "Lokalizacja",
                subtitle = "Zadania oparte o miejsce"
            )
            SettingsItem(
                icon = Icons.Default.Backup,
                title = "Kopia zapasowa",
                subtitle = "Synchronizacja z kontem Google"
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()

            Text(
                text = "ToDo App v1.0.0\nBuilt with Jetpack Compose",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            // KeyboardArrowRight to najlepszy zamiennik dla ChevronRight
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null
            )
        },
        modifier = Modifier.clickable { /* Tutaj dodasz akcje w przyszłości */ }
    )
}