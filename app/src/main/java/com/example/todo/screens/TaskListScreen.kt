package com.example.todo.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.todo.data.Priority
import com.example.todo.data.Task
import com.example.todo.data.TaskStatus
import com.example.todo.viewmodel.FilterOption
import com.example.todo.viewmodel.SortOption
import com.example.todo.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

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

    // search/local UI state
    var query by remember { mutableStateOf("") }
    LaunchedEffect(query) { viewModel.setSearchQuery(query) }

    // selected filter and sort (local mirrors to set vm)
    var filter by remember { mutableStateOf(FilterOption.ALL) }
    var sort by remember { mutableStateOf(SortOption.PRIORITY) }
    LaunchedEffect(filter) { viewModel.setFilterOption(filter) }
    LaunchedEffect(sort) { viewModel.setSortOption(sort) }

    val snackbarHostState = remember { SnackbarHostState() }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Moje Zadania") },
                actions = {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Sortuj")
                    }
                    DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Sortuj: Priorytet") }, onClick = { sort = SortOption.PRIORITY; sortMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Sortuj: Termin") }, onClick = { sort = SortOption.DUE_DATE; sortMenuExpanded = false })
                        DropdownMenuItem(text = { Text("Sortuj: Data utworzenia") }, onClick = { sort = SortOption.CREATED_AT; sortMenuExpanded = false })
                    }

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
    ) { padding ->
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

            // Filter chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipSelectable("Wszystko", FilterOption.ALL, filter) { filter = it }
                FilterChipSelectable("Na dziś", FilterOption.TODAY, filter) { filter = it }
                FilterChipSelectable("Zaległe", FilterOption.OVERDUE, filter) { filter = it }
                FilterChipSelectable("Wysoki priorytet", FilterOption.HIGH_PRIORITY, filter) { filter = it }
                FilterChipSelectable("Oczekujące", FilterOption.PENDING, filter) { filter = it }
                FilterChipSelectable("Wykonane", FilterOption.DONE, filter) { filter = it }
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
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
                                        viewModel.delete(task)
                                        val res = snackbarHostState.showSnackbar("Usunięto zadanie", actionLabel = "Cofnij", duration = SnackbarDuration.Short)
                                        if (res == SnackbarResult.ActionPerformed) {
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
                                TaskRowPriorityIndicator(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipSelectable(label: String, option: FilterOption, current: FilterOption, onSelected: (FilterOption) -> Unit) {
    val selected = option == current
    FilterChip(
        selected = selected,
        onClick = { onSelected(option) },
        label = { Text(label) },
        modifier = Modifier.defaultMinSize(minHeight = 40.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskRowPriorityIndicator(task: Task, onClick: () -> Unit, onToggleDone: () -> Unit) {
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
            leadingContent = {
                // Priority color dot
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(color = priorityColor(task.priority), shape = CircleShape)
                )
            },
            trailingContent = {
                Column(horizontalAlignment = Alignment.End) {
                    Checkbox(
                        checked = task.status == TaskStatus.DONE,
                        onCheckedChange = { onToggleDone() }
                    )
                    // optional: show due date or priority name below checkbox
                    task.dueAt?.let {
                        val formatted = java.text.DateFormat.getDateInstance().format(java.util.Date(it))
                        Text(formatted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        )
    }
}

private fun priorityColor(p: Priority): Color {
    return when (p) {
        Priority.HIGH -> Color(0xFFEF4444)     // red-500
        Priority.MEDIUM -> Color(0xFFF59E0B)   // amber-500
        Priority.LOW -> Color(0xFF10B981)      // green-500
    }
}