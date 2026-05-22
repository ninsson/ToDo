package com.example.todo.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.todo.data.Priority
import com.example.todo.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(navController: NavController, viewModel: TaskViewModel, taskId: Long) {
    var taskState by remember { mutableStateOf<com.example.todo.data.Task?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(taskId) {
        viewModel.getById(taskId) { t -> taskState = t }
    }

    val task = taskState ?: return

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("Szczegóły") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("edit/${task.id}") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edytuj")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.delete(task)
                            navController.navigateUp()
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(task.title, style = MaterialTheme.typography.headlineMedium)
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(onClick = {}, label = { Text("Status: ${task.status}") })

                    // Pojedynczy neutralny element z kolorową kropką wskazującą priorytet
                    PriorityIndicatorSimple(priority = task.priority)
                }
            }

            Divider()

            Text(
                text = task.description ?: "Brak opisu",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Location
            task.locationLat?.let { lat ->
                task.locationLng?.let { lng ->
                    Text("Lokalizacja: %.5f, %.5f".format(lat, lng), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(task.title)})")
                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                        context.startActivity(mapIntent)
                    }) {
                        Text("Pokaż w mapach / trasa")
                    }
                }
            }

            // Recurrence
            Text("Cykliczność: ${task.recurringRule ?: "Brak"}", style = MaterialTheme.typography.bodyMedium)

            // Attachments list
            if (task.attachments.isNotEmpty()) {
                Text("Załączniki", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    task.attachments.forEach { att ->
                        Text(
                            text = att.name ?: att.uri,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(att.uri)
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    context.startActivity(intent)
                                },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Divider()
            }

            Spacer(Modifier.weight(1f))

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Informacje o zadaniu", style = MaterialTheme.typography.labelLarge)
                    Text("Utworzono: ${DateFormat.getDateTimeInstance().format(Date(task.createdAt))}", style = MaterialTheme.typography.bodySmall)
                    task.dueAt?.let {
                        Text("Termin: ${DateFormat.getDateTimeInstance().format(Date(it))}", style = MaterialTheme.typography.bodySmall)
                    }
                    task.reminderTimeMillis?.let {
                        Text("Przypomnienie: ${DateFormat.getDateTimeInstance().format(Date(it))}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PriorityIndicatorSimple(priority: Priority) {
    val label = when (priority) {
        Priority.LOW -> "Niski"
        Priority.MEDIUM -> "Średni"
        Priority.HIGH -> "Wysoki"
    }
    val dotColor = when (priority) {
        Priority.LOW -> Color(0xFF10B981)
        Priority.MEDIUM -> Color(0xFFF59E0B)
        Priority.HIGH -> Color(0xFFEF4444)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .height(IntrinsicSize.Min)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color = dotColor, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}