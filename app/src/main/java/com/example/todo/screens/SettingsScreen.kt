package com.example.todo.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.todo.settings.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    val notificationsEnabled by repo.notificationsEnabled.collectAsState(initial = true)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ustawienia", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wstecz"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // Sekcja: Ogólne
            Column {
                Text(
                    text = "Ogólne",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    ListItem(
                        headlineContent = { Text("Powiadomienia") },
                        supportingContent = { Text("Zarządzaj alertami i dźwiękiem") },
                        leadingContent = {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch { repo.setNotificationsEnabled(checked) }
                                }
                            )
                        },
                        // Aby wiersz z Switchem też reagował na kliknięcie w dowolnym miejscu:
                        modifier = Modifier.clickable {
                            scope.launch { repo.setNotificationsEnabled(!notificationsEnabled) }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsItem(
                        icon = Icons.Default.LocationOn,
                        title = "Lokalizacja",
                        subtitle = "Zadania oparte o miejsce",
                        onClick = { /* TODO: Akcja dla lokalizacji */ }
                    )
                }
            }

            // Sekcja: Dane i synchronizacja
            Column {
                Text(
                    text = "Konto i dane",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    SettingsItem(
                        icon = Icons.Default.Backup,
                        title = "Kopia zapasowa",
                        subtitle = "Synchronizacja z kontem Google",
                        onClick = { /* TODO: Akcja dla kopii zapasowej */ }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
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
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable { onClick() }
    )
}