package com.example.todo.screens

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.todo.data.Attachment
import com.example.todo.data.Task
import com.example.todo.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(navController: NavController, viewModel: TaskViewModel, taskId: Long?) {
    val scope = rememberCoroutineScope()
    var task by remember { mutableStateOf<Task?>(null) }
    val context = LocalContext.current

    LaunchedEffect(taskId) {
        if (taskId != null) {
            viewModel.getById(taskId) { t -> task = t }
        } else {
            task = Task(title = "", description = "")
        }
    }

    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    LaunchedEffect(task) {
        task?.let {
            title = it.title
            desc = it.description ?: ""
        }
    }

    // Launcher for picking documents (attachments)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            // take persistable permission if possible
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { /* ignore */ }

            val mime = context.contentResolver.getType(uri)
            val name = queryDisplayName(context.contentResolver, uri)
            val att = Attachment(uri.toString(), mime, name)
            task = task?.copy(attachments = task?.attachments?.plus(att) ?: listOf(att))
        }
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
                val updatedTask = task?.copy(title = title, description = desc) ?: Task(title = title, description = desc)
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
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            // Attachments block
            Text("Załączniki", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    // allow any mime type (or restrict as needed)
                    launcher.launch(arrayOf("*/*"))
                }) {
                    Text("Dodaj załącznik")
                }
                task?.attachments?.let { list ->
                    Text("${list.size} załączników", modifier = Modifier.alignByBaseline())
                }
            }

            task?.attachments?.forEach { att ->
                Text(att.name ?: att.uri, style = MaterialTheme.typography.bodySmall)
            }

            Text("Wskazówka: Material 3 preferuje OutlinedTextField dla formularzy.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

// helper: query display name
private val IntentFlagsFlagsForUri = (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)

private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
    var name: String? = null
    val cursor: Cursor? = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            name = it.getString(0)
        }
    }
    return name
}