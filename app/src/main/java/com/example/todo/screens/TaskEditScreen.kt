package com.example.todo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
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

    var title by remember { mutableStateOf(task?.title ?: "") }
    var desc by remember { mutableStateOf(task?.description ?: "") }

    LaunchedEffect(task) {
        title = task?.title ?: ""
        desc = task?.description ?: ""
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text(if (taskId == null) "Create Task" else "Edit Task") })
    }, floatingActionButton = {
        FloatingActionButton(onClick = {
            val t = task?.copy(title = title, description = desc) ?: Task(title = title, description = desc)
            scope.launch {
                if (t.id == 0L) {
                    viewModel.create(t) { id -> navController.navigate("details/$id") }
                } else {
                    viewModel.update(t)
                    navController.navigateUp()
                }
            }
        }) {
            Icon(imageVector = Icons.Filled.Done, contentDescription = "Save")
        }
    }) { padding ->
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), maxLines = 6)
            Spacer(Modifier.height(8.dp))
            Text("TODO: add date/time, location, attachments, recurring options", style = MaterialTheme.typography.bodySmall)
        }
    }
}