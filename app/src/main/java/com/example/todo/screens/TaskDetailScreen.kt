package com.example.todo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.todo.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(navController: NavController, viewModel: TaskViewModel, taskId: Long) {
    var taskState by remember { mutableStateOf<com.example.todo.data.Task?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(taskId) {
        viewModel.getById(taskId) { t -> taskState = t }
    }

    val task = taskState ?: return

    Scaffold(topBar = {
        TopAppBar(title = { Text("Details") }, actions = {
            IconButton(onClick = { navController.navigate("edit/${task.id}") }) {
                Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit")
            }
        })
    }) { padding ->
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Text(task.title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(task.description ?: "-", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text("Priority: ${task.priority}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text("Status: ${task.status}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text("Created: ${DateFormat.getDateTimeInstance().format(Date(task.createdAt))}", style = MaterialTheme.typography.bodySmall)
            task.dueAt?.let { Text("Due: ${DateFormat.getDateTimeInstance().format(Date(it))}", style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(8.dp))
            if (task.attachments.isNotEmpty()) {
                Text("Attachments:", style = MaterialTheme.typography.bodySmall)
                task.attachments.forEach { att ->
                    Text(att.name ?: att.uri, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { scope.launch { viewModel.delete(task); navController.navigateUp() }}) {
                Text("Delete")
            }
        }
    }
}