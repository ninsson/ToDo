package com.example.todo.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.todo.data.Attachment
import com.example.todo.data.Priority
import com.example.todo.data.Task
import com.example.todo.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.util.*
import com.google.android.gms.location.LocationServices
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(navController: NavController, viewModel: TaskViewModel, taskId: Long?) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var task by remember { mutableStateOf<Task?>(null) }

    LaunchedEffect(taskId) {
        if (taskId != null) {
            viewModel.getById(taskId) { t -> task = t }
        } else {
            task = Task(title = "", description = "")
        }
    }

    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var recurrence by remember { mutableStateOf(task?.recurringRule ?: "NONE") }
    var reminderMillis by remember { mutableStateOf<Long?>(null) }
    var dueMillis by remember { mutableStateOf<Long?>(null) }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }

    LaunchedEffect(task) {
        task?.let {
            title = it.title
            desc = it.description ?: ""
            recurrence = it.recurringRule ?: "brak"
            reminderMillis = it.reminderTimeMillis
            dueMillis = it.dueAt
            priority = it.priority
        }
    }

    // Attachments launcher
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

    // Permission launcher for location
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // result handled inline when fetching location
    }

    // Date + time pickers helper
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
            FloatingActionButton(onClick = {
                val updatedTask = task?.copy(
                    title = title,
                    description = desc,
                    recurringRule = if (recurrence == "brak") null else recurrence,
                    reminderTimeMillis = reminderMillis,
                    dueAt = dueMillis,
                    priority = priority
                ) ?: Task(
                    title = title,
                    description = desc,
                    recurringRule = if (recurrence == "brak") null else recurrence,
                    reminderTimeMillis = reminderMillis,
                    dueAt = dueMillis,
                    priority = priority
                )

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
            }) {
                Icon(Icons.Default.Done, contentDescription = "Zapisz")
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(16.dp)
            .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Tytuł") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = desc,
                onValueChange = { desc = it },
                label = { Text("Opis (opcjonalnie)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            // Priority selection (colored chips)
            Text("Priorytet:", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PriorityChip(Priority.LOW, priority == Priority.LOW) { priority = it }
                PriorityChip(Priority.MEDIUM, priority == Priority.MEDIUM) { priority = it }
                PriorityChip(Priority.HIGH, priority == Priority.HIGH) { priority = it }
            }

            // Due date picker
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Termin:", modifier = Modifier.alignByBaseline())
                if (dueMillis != null) {
                    Text(java.text.DateFormat.getDateTimeInstance().format(Date(dueMillis!!)))
                    TextButton(onClick = { dueMillis = null }) { Text("Usuń") }
                } else {
                    Text("brak")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { pickDateTime(dueMillis) { picked -> dueMillis = picked } }) {
                    Text("Ustaw termin")
                }
            }

            // Reminder picker (kept)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Przypomnienie:", modifier = Modifier.alignByBaseline())
                if (reminderMillis != null) {
                    Text(java.text.DateFormat.getDateTimeInstance().format(Date(reminderMillis!!)))
                    TextButton(onClick = { reminderMillis = null }) { Text("Usuń") }
                } else {
                    Text("brak")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { pickDateTime(reminderMillis) { picked -> reminderMillis = picked } }) {
                    Text("Ustaw datę/godzinę")
                }
            }

            // Attachments block
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { pickDocumentLauncher.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Dodaj załącznik")
                }
                Text("${task?.attachments?.size ?: 0} załączników")
            }

            task?.attachments?.let { list ->
                if (list.isNotEmpty()) {
                    LazyColumn {
                        items(list) { att ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(att.name ?: att.uri, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(att.mimeType ?: "unknown", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                }
                                TextButton(onClick = {
                                    task = task?.copy(attachments = task!!.attachments.filter { it.uri != att.uri })
                                }) {
                                    Text("Usuń")
                                }
                            }
                        }
                    }
                }
            }

            // Location block
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null)
                Column {
                    val lat = task?.locationLat
                    val lng = task?.locationLng
                    Text(if (lat != null && lng != null) "Lokalizacja: %.5f, %.5f".format(lat, lng) else "Brak lokalizacji")
                    Row {
                        Button(onClick = {
                            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            val pm = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            if (pm == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                val fused = LocationServices.getFusedLocationProviderClient(context)
                                fused.lastLocation.addOnSuccessListener { loc ->
                                    loc?.let {
                                        task = task?.copy(locationLat = it.latitude, locationLng = it.longitude)
                                    }
                                }
                            }
                        }) {
                            Text("Ustaw na aktualną")
                        }
                        Spacer(Modifier.width(8.dp))
                        if (task?.locationLat != null) {
                            TextButton(onClick = { task = task?.copy(locationLat = null, locationLng = null) }) {
                                Text("Usuń lokalizację")
                            }
                        }
                    }
                }
            }

            // Recurrence selection
            val recurrenceOptions = listOf("brak", "codziennie", "co tydzień", "co miesiąc")
            var recurrenceMenuExpanded by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Powtarzalność:", modifier = Modifier.alignByBaseline())
                ExposedDropdownMenuBox(expanded = recurrenceMenuExpanded, onExpandedChange = { recurrenceMenuExpanded = !recurrenceMenuExpanded }) {
                    OutlinedTextField(
                        value = recurrence,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Cykliczność") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recurrenceMenuExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = recurrenceMenuExpanded, onDismissRequest = { recurrenceMenuExpanded = false }) {
                        recurrenceOptions.forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = {
                                recurrence = opt
                                task = task?.copy(recurringRule = if (opt == "brak") null else opt)
                                recurrenceMenuExpanded = false
                            })
                        }
                    }
                }
            }
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

private val IntentFlagsForUri = (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)

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