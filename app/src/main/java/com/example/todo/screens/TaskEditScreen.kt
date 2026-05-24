package com.example.todo.screens

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
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
import com.example.todo.data.TaskLocation
import com.example.todo.viewmodel.TaskViewModel
import com.example.todo.settings.SettingsRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(navController: NavController, viewModel: TaskViewModel, taskId: Long?) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val settingsRepo = remember { SettingsRepository(context) }
    val categories by settingsRepo.categories.collectAsState(initial = listOf("Praca", "Osobiste", "Zakupy", "Zdrowie", "Inne"))

    var task by remember { mutableStateOf<Task?>(null) }

    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    var recurrence by remember { mutableStateOf("brak") } // surowa reguła
    var recurrenceLabel by remember { mutableStateOf("Brak") }

    var reminderMillis by remember { mutableStateOf<Long?>(null) }
    var dueMillis by remember { mutableStateOf<Long?>(null) }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }

    // category state
    var category by remember { mutableStateOf<String?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    // dialog - niestandardowa reguła
    var showCustomRecurrenceDialog by remember { mutableStateOf(false) }
    var customCount by remember { mutableStateOf("1") }
    var customUnit by remember { mutableStateOf("dni") }

    // ADDRESS input states
    var addressInput by remember { mutableStateOf("") }
    var geocodeLoading by remember { mutableStateOf(false) }
    var geocodeError by remember { mutableStateOf<String?>(null) }

    fun ruleToDisplay(rule: String?): String {
        if (rule.isNullOrBlank() || rule == "brak") return "Brak"
        return when (rule) {
            "codziennie" -> "Codziennie"
            "co tydzień" -> "Co tydzień"
            "co miesiąc" -> "Co miesiąc"
            else -> {
                if (rule.startsWith("every:")) {
                    val parts = rule.split(":")
                    if (parts.size >= 3) {
                        val n = parts[1].toIntOrNull() ?: return rule
                        val unitKey = parts[2]
                        val unitPol = when (unitKey) {
                            "days" -> if (n == 1) "dzień" else "dni"
                            "weeks" -> if (n == 1) "tydzień" else "tygodnie"
                            "months" -> if (n == 1) "miesiąc" else "miesiące"
                            else -> unitKey
                        }
                        return if (n == 1) {
                            when (unitKey) {
                                "days" -> "Codziennie"
                                "weeks" -> "Co tydzień"
                                "months" -> "Co miesiąc"
                                else -> "Co $n $unitPol"
                            }
                        } else {
                            "Co $n $unitPol"
                        }
                    } else {
                        rule
                    }
                } else {
                    rule
                }
            }
        }
    }

    fun parseEveryRule(rule: String?): Pair<String, String>? {
        if (rule == null) return null
        if (!rule.startsWith("every:")) return null
        val parts = rule.split(":")
        if (parts.size < 3) return null
        val n = parts[1]
        val unitKey = parts[2]
        val unitPol = when (unitKey) {
            "days" -> "dni"
            "weeks" -> "tygodnie"
            "months" -> "miesiące"
            else -> "dni"
        }
        return Pair(n, unitPol)
    }

    LaunchedEffect(taskId) {
        if (taskId != null) {
            viewModel.getById(taskId) { t ->
                task = t
                title = t?.title ?: ""
                desc = t?.description ?: ""
                recurrence = t?.recurringRule ?: "brak"
                recurrenceLabel = ruleToDisplay(recurrence)
                reminderMillis = t?.reminderTimeMillis
                dueMillis = t?.dueAt
                priority = t?.priority ?: Priority.MEDIUM
                category = t?.category
                parseEveryRule(recurrence)?.let { (count, unitPol) ->
                    customCount = count
                    customUnit = unitPol
                }
            }
        } else {
            task = Task(title = "", description = "")
            recurrence = "brak"
            recurrenceLabel = ruleToDisplay(recurrence)
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
                    loc?.let {
                        val newLoc = TaskLocation(it.latitude, it.longitude, 100f, "Aktualna lokalizacja")
                        task = task?.copy(locations = (task?.locations ?: emptyList()) + newLoc)
                    }
                }
            } catch (e: SecurityException) { }
        }
    }

    // Launcher do otwierania MapPickActivity i odbierania wyniku
    val mapPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val lat = data?.getDoubleExtra("lat", Double.NaN) ?: Double.NaN
            val lng = data?.getDoubleExtra("lng", Double.NaN) ?: Double.NaN
            if (!lat.isNaN() && !lng.isNaN()) {
                val newLoc = TaskLocation(lat, lng, 100f, "Wybrane miejsce")
                task = task?.copy(locations = (task?.locations ?: emptyList()) + newLoc)
            }
        }
    }

    // Funkcja geokodująca wpisany adres
    fun addLocationFromAddress(addressText: String) {
        if (addressText.isBlank()) {
            geocodeError = "Podaj adres"
            return
        }
        geocodeError = null
        geocodeLoading = true
        scope.launch {
            val addresses = withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    geocoder.getFromLocationName(addressText, 3)
                } catch (e: Exception) {
                    null
                }
            }
            geocodeLoading = false
            if (addresses == null) {
                geocodeError = "Błąd geokodowania"
                Toast.makeText(context, "Błąd geokodowania. Spróbuj ponownie.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (addresses.isEmpty()) {
                geocodeError = "Nie znaleziono adresu"
                Toast.makeText(context, "Nie znaleziono adresu", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val adr = addresses.first()
            val lat = adr.latitude
            val lng = adr.longitude
            val label = adr.getAddressLine(0) ?: addressText
            val newLoc = TaskLocation(lat, lng, 100f, label)
            task = task?.copy(locations = (task?.locations ?: emptyList()) + newLoc)
            addressInput = ""
            geocodeError = null
            Toast.makeText(context, "Dodano lokalizację: $label", Toast.LENGTH_SHORT).show()
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
                        priority = priority,
                        category = category
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

            Column {
                Text("Kategoria", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = category ?: "Brak",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        DropdownMenuItem(text = { Text("Brak") }, onClick = { category = null; categoryMenuExpanded = false })
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = {
                                category = cat
                                categoryMenuExpanded = false
                            })
                        }
                    }
                }
            }

            // Priorytet
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

                    // Powtarzalność
                    var recurrenceMenuExpanded by remember { mutableStateOf(false) }
                    val recurrenceOptions = listOf("brak", "codziennie", "co tydzień", "co miesiąc", "własny")

                    ExposedDropdownMenuBox(
                        expanded = recurrenceMenuExpanded,
                        onExpandedChange = { recurrenceMenuExpanded = !recurrenceMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = recurrenceLabel,
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
                                            parseEveryRule(recurrence)?.let { (count, unitPol) ->
                                                customCount = count
                                                customUnit = unitPol
                                            }
                                            showCustomRecurrenceDialog = true
                                        } else {
                                            recurrence = if (opt == "brak") "brak" else opt
                                            recurrenceLabel = ruleToDisplay(recurrence)
                                            task = task?.copy(recurringRule = if (recurrence == "brak") null else recurrence)
                                            if (dueMillis == null && recurrence != "brak") dueMillis = System.currentTimeMillis()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    if (showCustomRecurrenceDialog) {
                        AlertDialog(
                            onDismissRequest = { showCustomRecurrenceDialog = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    val n = customCount.toLongOrNull() ?: 0L
                                    if (n <= 0L) {
                                        return@TextButton
                                    }
                                    val unitKey = when (customUnit) {
                                        "dni" -> "days"
                                        "tygodnie" -> "weeks"
                                        "miesiące" -> "months"
                                        else -> "days"
                                    }
                                    val rule = "every:$n:$unitKey"
                                    recurrence = rule
                                    recurrenceLabel = ruleToDisplay(rule)
                                    task = task?.copy(recurringRule = rule)
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
                                            onValueChange = { v -> customCount = v.filter { it.isDigit() } },
                                            label = { Text("Co ile") },
                                            singleLine = true,
                                            modifier = Modifier.width(120.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
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

            // Załączniki
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

            // --- ZAKTUALIZOWANA SEKCJA: LOKALIZACJE ---
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Nagłówek sekcji
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Lokalizacje", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pole adresu (z wyszukiwaniem przeniesionym do środka)
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Znajdź po adresie...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (geocodeLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(
                                    onClick = { addLocationFromAddress(addressInput) },
                                    enabled = addressInput.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Dodaj z adresu", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    )

                    geocodeError?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Przyciski szybkich akcji (pół na pół)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = {
                                val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                                    val fused = LocationServices.getFusedLocationProviderClient(context)
                                    fused.lastLocation.addOnSuccessListener { loc ->
                                        loc?.let {
                                            val newLoc = TaskLocation(it.latitude, it.longitude, 100f, "Aktualna lokalizacja")
                                            task = task?.copy(locations = (task?.locations ?: emptyList()) + newLoc)
                                        }
                                    }
                                } else {
                                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Moja pozycja", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(context, MapPickActivity::class.java)
                                mapPickerLauncher.launch(intent)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Wybierz", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Lista dodanych lokalizacji
                    val locations = task?.locations ?: emptyList()
                    if (locations.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Brak przypisanych lokalizacji", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            locations.forEachIndexed { idx, loc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(loc.label ?: "Miejsce ${idx + 1}", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("%.5f, %.5f".format(loc.lat, loc.lng), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Row {
                                        IconButton(onClick = {
                                            val gmmIntentUri = Uri.parse("geo:${loc.lat},${loc.lng}?q=${loc.lat},${loc.lng}(${Uri.encode(title)})")
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            context.startActivity(mapIntent)
                                        }) {
                                            Icon(Icons.Default.Map, contentDescription = "Otwórz w Mapach", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = {
                                            task = task?.copy(locations = locations.filterIndexed { i, _ -> i != idx })
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // --- KONIEC ZAKTUALIZOWANEJ SEKCJI ---

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