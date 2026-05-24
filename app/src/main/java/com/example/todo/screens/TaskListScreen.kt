package com.example.todo.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(navController: NavController, viewModel: TaskViewModel) {
    val tasks by viewModel.visibleTasks.collectAsState()
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    LaunchedEffect(query) { viewModel.setSearchQuery(query) }

    var filter by remember { mutableStateOf(FilterOption.ALL) }
    var sort by remember { mutableStateOf(SortOption.PRIORITY) }
    LaunchedEffect(filter) { viewModel.setFilterOption(filter) }
    LaunchedEffect(sort) { viewModel.setSortOption(sort) }

    val snackbarHostState = remember { SnackbarHostState() }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Moje Zadania", fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sortuj")
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            DropdownMenuItem(text = { Text("Sortuj: Priorytet") }, onClick = { sort = SortOption.PRIORITY; sortMenuExpanded = false })
                            DropdownMenuItem(text = { Text("Sortuj: Termin") }, onClick = { sort = SortOption.DUE_DATE; sortMenuExpanded = false })
                            DropdownMenuItem(text = { Text("Sortuj: Data utworzenia") }, onClick = { sort = SortOption.CREATED_AT; sortMenuExpanded = false })
                        }
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Ustawienia")
                    }
                },
//                modifier = Modifier.statusBarsPadding()
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("create") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Dodaj zadanie") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Pasek wyszukiwania
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Szukaj zadań...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Filtry przewijane w poziomie
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { FilterChipSelectable("Wszystko", FilterOption.ALL, filter) { filter = it } }
                item { FilterChipSelectable("Na dziś", FilterOption.TODAY, filter) { filter = it } }
                item { FilterChipSelectable("Zaległe", FilterOption.OVERDUE, filter) { filter = it } }
                item { FilterChipSelectable("Wysoki priorytet", FilterOption.HIGH_PRIORITY, filter) { filter = it } }
                item { FilterChipSelectable("Oczekujące", FilterOption.PENDING, filter) { filter = it } }
                item { FilterChipSelectable("Wykonane", FilterOption.DONE, filter) { filter = it } }
            }

            if (tasks.isEmpty()) {
                // Ładniejszy widok pustej listy
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp).alpha(0.2f),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (query.isNotEmpty()) "Brak wyników wyszukiwania" else "Brak zadań. Dodaj coś!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 80.dp, // Miejsce na pływający przycisk
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val dismissState = rememberDismissState(confirmStateChange = { state ->
                            when (state) {
                                DismissValue.DismissedToEnd -> {
                                    if (task.status == TaskStatus.DONE) {
                                        scope.launch { viewModel.reopenTask(task) }
                                    } else {
                                        scope.launch { viewModel.completeTask(task) }
                                    }
                                    false // Odskakuje z powrotem, bo tylko zmieniamy stan (nie usuwamy elementu z listy permanentnie)
                                }
                                DismissValue.DismissedToStart -> {
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
                            background = {
                                val direction = dismissState.dismissDirection ?: return@SwipeToDismiss
                                val color by animateColorAsState(
                                    when (dismissState.targetValue) {
                                        DismissValue.DismissedToEnd -> Color(0xFF10B981) // Zielony na wykonanie
                                        DismissValue.DismissedToStart -> MaterialTheme.colorScheme.error // Czerwony na usunięcie
                                        else -> Color.Transparent
                                    }
                                )
                                val alignment = when (direction) {
                                    DismissDirection.StartToEnd -> Alignment.CenterStart
                                    DismissDirection.EndToStart -> Alignment.CenterEnd
                                }
                                val icon = when (direction) {
                                    DismissDirection.StartToEnd -> Icons.Default.Check
                                    DismissDirection.EndToStart -> Icons.Default.Delete
                                }

                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = alignment
                                ) {
                                    Icon(icon, contentDescription = null, tint = Color.White)
                                }
                            },
                            dismissContent = {
                                TaskRowPriorityIndicator(
                                    task = task,
                                    onClick = { navController.navigate("details/${task.id}") },
                                    onToggleDone = {
                                        if (task.status == TaskStatus.DONE) {
                                            scope.launch { viewModel.reopenTask(task) }
                                        } else {
                                            scope.launch { viewModel.completeTask(task) }
                                        }
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
private fun FilterChipSelectable(label: String, option: FilterOption, current: FilterOption, onSelected: (FilterOption) -> Unit) {
    val selected = option == current
    FilterChip(
        selected = selected,
        onClick = { onSelected(option) },
        label = { Text(label) },
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.defaultMinSize(minHeight = 40.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskRowPriorityIndicator(task: Task, onClick: () -> Unit, onToggleDone: () -> Unit) {
    val isDone = task.status == TaskStatus.DONE
    val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kropka priorytetu
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color = if (isDone) Color.Gray else priorityColor(task.priority), shape = CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Tekst: Tytuł i ewentualny opis/data
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                    color = if (isDone) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    if (task.dueAt != null) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isDone) Color.Gray else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dateFormatter.format(Date(task.dueAt!!)),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDone) Color.Gray else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (!task.description.isNullOrBlank()) {
                        Text(
                            text = task.description!!,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = if (isDone) TextDecoration.LineThrough else null
                        )
                    }
                }
            }

            // Checkbox
            Checkbox(
                checked = isDone,
                onCheckedChange = { onToggleDone() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

private fun priorityColor(p: Priority): Color {
    return when (p) {
        Priority.HIGH -> Color(0xFFEF4444)     // Czerwony
        Priority.MEDIUM -> Color(0xFFF59E0B)   // Pomarańczowy
        Priority.LOW -> Color(0xFF10B981)      // Zielony
    }
}