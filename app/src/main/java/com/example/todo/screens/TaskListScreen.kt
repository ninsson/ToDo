package com.example.todo.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.todo.data.Task
import com.example.todo.data.TaskStatus
import com.example.todo.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

// Compose Material (stable) swipe imports (wymagają opt-in)
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material.ExperimentalMaterialApi

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(navController: NavController, viewModel: TaskViewModel) {
    val tasks by viewModel.visibleTasks.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // search
    var query by remember { mutableStateOf("") }
    LaunchedEffect(query) { viewModel.setSearchQuery(query) }

    // Snackbar host for UNDO
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Moje Zadania") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ustawienia")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("create") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Dodaj zadanie") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding -> // Ten padding zawiera w sobie wysokość paska stanu i top bara!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Szukaj...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("Brak zadań. Dodaj coś!", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = padding.calculateBottomPadding() + 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val dismissState = rememberDismissState(confirmStateChange = { state ->
                            when (state) {
                                DismissValue.DismissedToEnd -> {
                                    // Right swipe -> toggle done
                                    val newStatus = if (task.status == TaskStatus.DONE) TaskStatus.PENDING else TaskStatus.DONE
                                    scope.launch { viewModel.update(task.copy(status = newStatus)) }
                                    true
                                }
                                DismissValue.DismissedToStart -> {
                                    // Left swipe -> delete with UNDO
                                    scope.launch {
                                        // wykonaj delete
                                        viewModel.delete(task)
                                        // pokaż snackbar z możliwością cofnięcia
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Usunięto zadanie",
                                            actionLabel = "Cofnij",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            // Cofnij: wstaw zadanie ponownie jako nowy rekord (id = 0)
                                            viewModel.create(task.copy(id = 0L))
                                        }
                                    }
                                    true
                                }
                                else -> false
                            }
                        })

                        SwipeToDismiss(
                            state = dismissState,
                            directions = setOf(DismissDirection.StartToEnd, DismissDirection.EndToStart),
                            background = {},
                            dismissContent = {
                                TaskRow(
                                    task = task,
                                    onClick = { navController.navigate("details/${task.id}") },
                                    onToggleDone = {
                                        val newStatus = if (task.status == TaskStatus.DONE) TaskStatus.PENDING else TaskStatus.DONE
                                        scope.launch { viewModel.update(task.copy(status = newStatus)) }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskRow(task: Task, onClick: () -> Unit, onToggleDone: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text(task.title, style = MaterialTheme.typography.titleMedium) },
            supportingContent = {
                task.description?.let {
                    Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            trailingContent = {
                Checkbox(
                    checked = task.status == TaskStatus.DONE,
                    onCheckedChange = { onToggleDone() }
                )
            }
        )
    }
}