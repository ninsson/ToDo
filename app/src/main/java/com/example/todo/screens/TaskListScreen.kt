package com.example.todo.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.todo.data.Task
import com.example.todo.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(navController: NavController, viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ToDo") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("create") }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(tasks) { task ->
                TaskRow(
                    task = task,
                    onClick = { navController.navigate("details/${task.id}") },
                    onToggleDone = {
                        val t = task.copy(
                            status = if (task.status == com.example.todo.data.TaskStatus.DONE)
                                com.example.todo.data.TaskStatus.PENDING
                            else com.example.todo.data.TaskStatus.DONE
                        )
                        scope.launch { viewModel.update(t) }
                    },
                    onSwipeDelete = { scope.launch { viewModel.delete(task) } }
                )
            }
        }
    }
}

@Composable
fun TaskRow(task: Task, onClick: () -> Unit, onToggleDone: () -> Unit, onSwipeDelete: () -> Unit) {
    Card(modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth()
        .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.titleMedium)
                task.description?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
            IconButton(onClick = onToggleDone) {
                Icon(
                    imageVector = if (task.status == com.example.todo.data.TaskStatus.DONE)
                        Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null
                )
            }
        }
    }
}