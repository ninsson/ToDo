package com.example.todo.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Close
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
    val categories by repo.categories.collectAsState(initial = emptyList())

    var newCategoryText by remember { mutableStateOf("") }
    val defaultCategories = listOf("Praca", "Osobiste", "Zakupy", "Zdrowie", "Inne")

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

            // Sekcja: Kategorie
            Column {
                Text(
                    text = "Kategorie",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Dostępne kategorie", style = MaterialTheme.typography.titleMedium)

                        FlowRowCategories(
                            categories = categories.ifEmpty { defaultCategories }, // Zabezpieczenie przed pustą listą
                            defaultList = defaultCategories,
                            onDelete = { cat -> scope.launch { repo.removeCategory(cat) } }
                        )

                        OutlinedTextField(
                            value = newCategoryText,
                            onValueChange = { newCategoryText = it },
                            label = { Text("Nowa kategoria") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        val txt = newCategoryText.trim()
                                        if (txt.isNotEmpty() && !categories.contains(txt)) {
                                            scope.launch {
                                                repo.addCategory(txt)
                                                newCategoryText = ""
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Dodaj kategorię",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCategories(
    categories: List<String>,
    defaultList: List<String>,
    onDelete: (String) -> Unit
) {
    // Używamy FlowRow z biblioteki layout, aby automatycznie zawijać elementy do nowej linii
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            val isDefault = cat in defaultList

            AssistChip(
                onClick = { /* Można dodać akcję po kliknięciu na kategorię */ },
                label = { Text(cat) },
                trailingIcon = if (!isDefault) {
                    {
                        IconButton(
                            onClick = { onDelete(cat) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Usuń",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else null
            )
        }
    }
}