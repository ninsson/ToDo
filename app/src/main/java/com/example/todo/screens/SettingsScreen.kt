package com.example.todo.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.todo.notifications.GeofenceManager
import com.example.todo.notifications.ReminderScheduler
import com.example.todo.settings.SettingsRepository
import com.example.todo.settings.ThemeMode
import kotlinx.coroutines.launch

/**
 * Ekran ustawień aplikacji użytkownika.
 *
 * Odpowiada za:
 * - Prezentację bieżących preferencji (motyw, powiadomienia, lokalizacja).
 * - Bezpośrednią kontrolę nad usługami systemowymi poprzez [GeofenceManager] i [ReminderScheduler].
 * - Zarządzanie słownikiem kategorii zadań.
 *
 * @param navController Kontroler nawigacji używany do obsługi przycisku "Wstecz".
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()

    val notificationsEnabled by repo.notificationsEnabled.collectAsState(initial = true)
    val locationEnabled by repo.locationEnabled.collectAsState(initial = true)
    val themeMode by repo.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
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
                        supportingContent = { Text("Przypomnienia z tej aplikacji") },
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
                                    scope.launch {
                                        repo.setNotificationsEnabled(checked)
                                        if (!checked) {
                                            ReminderScheduler.cancelAllReminders(context)
                                        }
                                    }
                                }
                            )
                        },
                        modifier = Modifier.clickable {
                            scope.launch {
                                val next = !notificationsEnabled
                                repo.setNotificationsEnabled(next)
                                if (!next) {
                                    ReminderScheduler.cancelAllReminders(context)
                                }
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    ListItem(
                        headlineContent = { Text("Lokalizacja") },
                        supportingContent = { Text("Dodawanie miejsc i nawigacja") },
                        leadingContent = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = locationEnabled,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        repo.setLocationEnabled(checked)
                                        if (!checked) {
                                            GeofenceManager.removeAllGeofences(context)
                                        }
                                    }
                                }
                            )
                        },
                        modifier = Modifier.clickable {
                            scope.launch {
                                val next = !locationEnabled
                                repo.setLocationEnabled(next)
                                if (!next) {
                                    GeofenceManager.removeAllGeofences(context)
                                }
                            }
                        }
                    )
                }
            }

            // Sekcja: Wygląd
            Column {
                Text(
                    text = "Wygląd",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Tryb motywu", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = themeMode == ThemeMode.SYSTEM,
                                onClick = { scope.launch { repo.setThemeMode(ThemeMode.SYSTEM) } }
                            )
                            Text("Systemowy")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = themeMode == ThemeMode.LIGHT,
                                onClick = { scope.launch { repo.setThemeMode(ThemeMode.LIGHT) } }
                            )
                            Text("Jasny")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = themeMode == ThemeMode.DARK,
                                onClick = { scope.launch { repo.setThemeMode(ThemeMode.DARK) } }
                            )
                            Text("Ciemny")
                        }
                    }
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
                            categories = categories.ifEmpty { defaultCategories },
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

/**
 * Komponent wyświetlający kategorie w formie interaktywnych chipów.
 *
 * @param categories Lista aktualnych kategorii.
 * @param defaultList Lista kategorii wbudowanych (nienadających się do usunięcia).
 * @param onDelete Callback wywoływany przy próbie usunięcia niestandardowej kategorii.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCategories(
    categories: List<String>,
    defaultList: List<String>,
    onDelete: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            val isDefault = cat in defaultList

            AssistChip(
                onClick = { },
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