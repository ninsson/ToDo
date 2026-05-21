package com.example.todo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.todo.data.Task
import com.example.todo.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditScreen(navController: NavController, viewModel: TaskViewModel, taskId: Long?) {
    val scope = rememberCoroutineScope()
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

    LaunchedEffect(task) {
        task?.let {
            title = it.title
            desc = it.description ?: ""
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

            Text("Wskazówka: Material 3 preferuje OutlinedTextField dla formularzy.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}