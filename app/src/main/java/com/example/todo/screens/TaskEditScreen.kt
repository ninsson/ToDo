package com.example.todo.screens

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.todo.data.Attachment
import com.example.todo.data.Priority
import com.example.todo.data.Task
import com.example.todo.viewmodel.TaskViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(navController: NavController, viewModel: TaskViewModel, taskId: Long?) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var task by remember { mutableStateOf<Task?>(null) }

    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var recurrence by remember { mutableStateOf("brak") } // przechowuje wartość, która trafi do recurringRule (np. "codziennie" lub "every:3:days")
    var reminderMillis by remember { mutableStateOf<Long?>(null) }
    var dueMillis by remember { mutableStateOf<Long?>(null) }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }

    // dialog - niestandardowa reguła
    var showCustomRecurrenceDialog by remember { mutableStateOf(false) }
    var customCount by remember { mutableStateOf("1") }
    var customUnit by remember { mutableStateOf("dni") } // wyświetlany tekst, mapujemy do days/weeks/months

    LaunchedEffect(taskId) {
        if (taskId != null) {
            viewModel.getById(taskId) { t ->
                task = t
                title = t?.title ?: ""
                desc = t?.description ?: ""
                recurrence = t?.recurringRule ?: "brak"
                reminderMillis = t?.reminderTimeMillis
                dueMillis = t?.dueAt
                priority = t?.priority ?: Priority.MEDIUM
            }
        } else {
            task = Task(title = "", description = "")
        }
    }

    val pickDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            val mime = context.contentResolver.getType(it)
            val name = queryDisplayName(context.contentResolver, it) ?: it.toString()
            val att = Attachment(it.toString(), mime, name)
            task = task?.copy(attachments = (task?.attachments ?: emptyList()) + att)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            try {
                fused.lastLocation.addOnSuccessListener { loc ->
                    loc?.let { task = task?.copy(locationLat = it.latitude, locationLng = it.longitude) }
                }
            } catch (e: SecurityException) { /* Ignoruj, jeśli brak uprawnień mimo przyznania */ }
        }
    }

    fun pickDateTime(existingMillis: Long?, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        if (existingMillis != null) cal.timeInMillis = existingMillis

        DatePickerDialog(context, { _, y, m, d ->
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, m)
            cal.set(Calendar.DAY_OF_MONTH, d)
            TimePickerDialog(context, { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                onPicked(cal.timeInMillis)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == null) "Nowe zadanie" else "Edycja zadania") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val updatedTask = task?.copy(
                        title = title,
                        description = desc.takeIf { it.isNotBlank() },
                        recurringRule = if (recurrence == "brak") null else recurrence,
                        reminderTimeMillis = reminderMillis,
                        dueAt = dueMillis,
                        priority = priority
                    ) ?: return@FloatingActionButton

                    scope.launch {
                        if (updatedTask.id == 0L) {
                            viewModel.create(updatedTask) { id ->
                                navController.navigate("details/$id") {
                                    popUpTo("create") { inclusive = true }
                                }
                            }
                        } else {
                            viewModel.update(updatedTask)
                            navController.navigateUp()
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Done, contentDescription = "Zapisz", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Sekcja: Podstawowe informacje
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tytuł") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Opis (opcjonalnie)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )

            // Sekcja: Priorytet
            Column {
                Text("Priorytet", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PriorityChip(Priority.LOW, priority == Priority.LOW) { priority = it }
                    PriorityChip(Priority.MEDIUM, priority == Priority.MEDIUM) { priority = it }
                    PriorityChip(Priority.HIGH, priority == Priority.HIGH) { priority = it }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Sekcja: Daty i czas
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    // Termin
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Termin wykonania", fontWeight = FontWeight.Bold)
                                Text(dueMillis?.let { dateFormatter.format(Date(it)) } ?: "Brak terminu", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (dueMillis != null) {
                            IconButton(onClick = { dueMillis = null }) { Icon(Icons.Default.Clear, contentDescription = "Usuń") }
                        } else {
                            TextButton(onClick = { pickDateTime(dueMillis) { dueMillis = it } }) { Text("Ustaw") }
                        }
                    }

                    // Przypomnienie
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Przypomnienie", fontWeight = FontWeight.Bold)
                                Text(reminderMillis?.let { dateFormatter.format(Date(it)) } ?: "Brak przypomnienia", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (reminderMillis != null) {
                            IconButton(onClick = { reminderMillis = null }) { Icon(Icons.Default.Clear, contentDescription = "Usuń") }
                        } else {
                            TextButton(onClick = { pickDateTime(reminderMillis) { reminderMillis = it } }) { Text("Ustaw") }
                        }
                    }

                    // Cykliczność
                    var recurrenceMenuExpanded by remember { mutableStateOf(false) }
                    val recurrenceOptions = listOf("brak", "codziennie", "co tydzień", "co miesiąc", "własny")

                    ExposedDropdownMenuBox(
                        expanded = recurrenceMenuExpanded,
                        onExpandedChange = { recurrenceMenuExpanded = !recurrenceMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = recurrence,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Powtarzalność") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recurrenceMenuExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = recurrenceMenuExpanded,
                            onDismissRequest = { recurrenceMenuExpanded = false }
                        ) {
                            recurrenceOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        recurrenceMenuExpanded = false
                                        if (opt == "własny") {
                                            // otwórz dialog do podania niestandardowej reguły
                                            showCustomRecurrenceDialog = true
                                        } else {
                                            // normalny wybór
                                            recurrence = if (opt == "brak") "brak" else opt
                                            task = task?.copy(recurringRule = if (recurrence == "brak") null else recurrence)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Dialog: niestandardowa reguła
                    if (showCustomRecurrenceDialog) {
                        AlertDialog(
                            onDismissRequest = { showCustomRecurrenceDialog = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    val n = customCount.toLongOrNull() ?: 0L
                                    if (n <= 0L) {
                                        // ignoruj / nie zatwierdzaj jeśli niepoprawne
                                        return@TextButton
                                    }
                                    // przetłumacz jednostkę na klucz używany przez computeNextMillis
                                    val unitKey = when (customUnit) {
                                        "dni" -> "days"
                                        "tygodnie" -> "weeks"
                                        "miesiące" -> "months"
                                        else -> "days"
                                    }
                                    val rule = "every:$n:$unitKey"
                                    recurrence = rule
                                    // ustaw task.recurringRule
                                    task = task?.copy(recurringRule = rule)
                                    // jeśli nie ma dueMillis ustaw teraz, żeby od razu było widoczne w UI
                                    if (dueMillis == null) dueMillis = System.currentTimeMillis()
                                    showCustomRecurrenceDialog = false
                                }) {
                                    Text("OK")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCustomRecurrenceDialog = false }) {
                                    Text("Anuluj")
                                }
                            },
                            title = { Text("Niestandardowa powtarzalność") },
                            text = {
                                Column {
                                    Text("Podaj interwał powtarzania:")
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = customCount,
                                            onValueChange = { v ->
                                                // dopuść tylko cyfry
                                                customCount = v.filter { it.isDigit() }
                                            },
                                            label = { Text("Co ile") },
                                            singleLine = true,
                                            modifier = Modifier.width(120.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        // jednostka - prosty dropdown
                                        var unitMenuExpanded by remember { mutableStateOf(false) }
                                        Box {
                                            OutlinedTextField(
                                                value = customUnit,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Jednostka") },
                                                trailingIcon = {
                                                    IconButton(onClick = { unitMenuExpanded = true }) {
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                                    }
                                                },
                                                modifier = Modifier.width(160.dp)
                                            )
                                            DropdownMenu(expanded = unitMenuExpanded, onDismissRequest = { unitMenuExpanded = false }) {
                                                listOf("dni", "tygodnie", "miesiące").forEach { u ->
                                                    DropdownMenuItem(text = { Text(u) }, onClick = { customUnit = u; unitMenuExpanded = false })
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Sekcja: Załączniki
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Załączniki (${task?.attachments?.size ?: 0})", fontWeight = FontWeight.Bold)
                        TextButton(onClick = { pickDocumentLauncher.launch(arrayOf("*/*")) }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Dodaj")
                        }
                    }

                    task?.attachments?.forEach { att ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AttachFile, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(att.name ?: "Nieznany plik", maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                    Text(att.mimeType ?: "unknown", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            IconButton(onClick = {
                                task = task?.copy(attachments = task!!.attachments.filter { it.uri != att.uri })
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Sekcja: Lokalizacja
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val lat = task?.locationLat
                    val lng = task?.locationLng

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lokalizacja", fontWeight = FontWeight.Bold)
                            if (lat != null && lng != null) {
                                Text("%.5f, %.5f".format(lat, lng), style = MaterialTheme.typography.bodyMedium)
                            } else {
                                Text("Brak przypisanej lokalizacji", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        if (lat != null) {
                            TextButton(onClick = { task = task?.copy(locationLat = null, locationLng = null) }) {
                                Text("Usuń", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        TextButton(onClick = {
                            val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                val fused = LocationServices.getFusedLocationProviderClient(context)
                                fused.lastLocation.addOnSuccessListener { loc ->
                                    loc?.let { task = task?.copy(locationLat = it.latitude, locationLng = it.longitude) }
                                }
                            } else {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        }) {
                            Text("Ustaw aktualną")
                        }
                    }
                }
            }

            // Margines pod przyciskiem FAB
            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun PriorityChip(p: Priority, selected: Boolean, onSelect: (Priority) -> Unit) {
    val label = when (p) {
        Priority.LOW -> "Niski"
        Priority.MEDIUM -> "Średni"
        Priority.HIGH -> "Wysoki"
    }
    val color = when (p) {
        Priority.LOW -> Color(0xFF10B981)
        Priority.MEDIUM -> Color(0xFFF59E0B)
        Priority.HIGH -> Color(0xFFEF4444)
    }
    FilterChip(
        selected = selected,
        onClick = { onSelect(p) },
        label = { Text(label) },
        leadingIcon = {
            Box(modifier = Modifier
                .size(12.dp)
                .background(color = color, shape = CircleShape))
        },
        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
    )
}

private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
    var name: String? = null
    val cursor = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            name = it.getString(0)
        }
    }
    return name
}