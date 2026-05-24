package com.example.todo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.todo.data.Priority
import com.example.todo.data.Task
import com.example.todo.data.TaskStatus
import com.example.todo.notifications.GeofenceManager
import com.example.todo.notifications.ReminderScheduler
import com.example.todo.repo.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

enum class SortOption { PRIORITY, DUE_DATE, CREATED_AT }
enum class FilterOption { ALL, TODAY, OVERDUE, HIGH_PRIORITY, PENDING, DONE }

class TaskViewModel(private val repo: TaskRepository, private val context: Context? = null) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(SortOption.PRIORITY)
    private val _filterOption = MutableStateFlow(FilterOption.ALL)

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setSortOption(s: SortOption) { _sortOption.value = s }
    fun setFilterOption(f: FilterOption) { _filterOption.value = f }

    // combine repo.observeAll() with search/filter/sort
    val visibleTasks: StateFlow<List<Task>> = combine(
        repo.observeAll(),
        _searchQuery.debounce(250),
        _sortOption,
        _filterOption
    ) { list, q, sortOpt, filterOpt ->
        var result = list

        // search
        val query = q.trim().lowercase()
        if (query.isNotEmpty()) {
            result = result.filter { t ->
                t.title.lowercase().contains(query) || (t.description?.lowercase()?.contains(query) ?: false)
            }
        }

        // filter
        val now = System.currentTimeMillis()
        fun Long.toLocalDate(): LocalDate =
            Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

        result = when (filterOpt) {
            FilterOption.ALL -> result
            FilterOption.TODAY -> {
                val today = LocalDate.now()
                result.filter { it.dueAt != null && it.dueAt!!.toLocalDate() == today }
            }
            FilterOption.OVERDUE -> result.filter { it.dueAt != null && it.dueAt!! < now && it.status != TaskStatus.DONE }
            FilterOption.HIGH_PRIORITY -> result.filter { it.priority == Priority.HIGH }
            FilterOption.PENDING -> result.filter { it.status == TaskStatus.PENDING }
            FilterOption.DONE -> result.filter { it.status == TaskStatus.DONE }
        }

        // sort: use explicit Comparators to avoid lambda/generic inference issues
        result = when (sortOpt) {
            SortOption.PRIORITY -> {
                result.sortedWith(Comparator { a, b ->
                    // priority descending
                    val pCmp = b.priority.ordinal.compareTo(a.priority.ordinal)
                    if (pCmp != 0) return@Comparator pCmp
                    // then due date ascending (nulls go to end)
                    val da = a.dueAt ?: Long.MAX_VALUE
                    val db = b.dueAt ?: Long.MAX_VALUE
                    return@Comparator da.compareTo(db)
                })
            }
            SortOption.DUE_DATE -> {
                result.sortedWith(Comparator { a, b ->
                    val da = a.dueAt ?: Long.MAX_VALUE
                    val db = b.dueAt ?: Long.MAX_VALUE
                    val dCmp = da.compareTo(db)
                    if (dCmp != 0) return@Comparator dCmp
                    // then priority descending
                    return@Comparator b.priority.ordinal.compareTo(a.priority.ordinal)
                })
            }
            SortOption.CREATED_AT -> {
                result.sortedByDescending { it.createdAt }
            }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Backwards compat alias
    val tasks: StateFlow<List<Task>> = visibleTasks

    /**
     * Wstawienie zadania do bazy.
     * Jeśli zadanie jest powtarzalne i nie ma dueAt, ustawiamy dueAt = createdAt (żeby było widoczne w UI).
     */
    fun create(task: Task, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val taskToInsert = if (!task.recurringRule.isNullOrBlank() && task.dueAt == null) {
                // traktujemy createdAt jako początek serii i ustawiamy dueAt, żeby wyświetlało się w liście/przypomnieniu
                task.copy(dueAt = task.createdAt)
            } else {
                task
            }

            val id = repo.insert(taskToInsert)
            context?.let { ctx ->
                if (taskToInsert.reminderTimeMillis != null) ReminderScheduler.scheduleReminder(ctx, taskToInsert.copy(id = id))
                if (taskToInsert.locationLat != null && taskToInsert.locationLng != null) GeofenceManager.addGeofenceForTask(ctx, id,
                    taskToInsert.locationLat!!,
                    taskToInsert.locationLng!!
                )
            }
            onDone(id)
        }
    }

    fun update(task: Task) {
        viewModelScope.launch {
            repo.update(task)
            context?.let { ctx ->
                ReminderScheduler.cancelReminder(ctx, task.id)
                if (task.reminderTimeMillis != null) ReminderScheduler.scheduleReminder(ctx, task)

                GeofenceManager.removeGeofenceForTask(ctx, task.id)
                if (task.locationLat != null && task.locationLng != null) {
                    GeofenceManager.addGeofenceForTask(ctx, task.id, task.locationLat!!,
                        task.locationLng!!
                    )
                }
            }
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            repo.delete(task)
            context?.let { ctx ->
                ReminderScheduler.cancelReminder(ctx, task.id)
                GeofenceManager.removeGeofenceForTask(ctx, task.id)
            }
        }
    }

    fun getById(id: Long, callback: (Task?) -> Unit) {
        viewModelScope.launch {
            callback(repo.getById(id))
        }
    }

    // ---------------------------
    // Recurrence helpers & actions
    // ---------------------------

    // Oblicza następny timestamp (millis) bazując na currentMillis i regule.
    // Obsługa prostych polskich nazw: "codziennie", "co tydzień", "co miesiąc"
    // Dodatkowo prosty własny format: "every:<n>:<unit>" gdzie unit in {days, weeks, months}
    private fun computeNextMillis(currentMillis: Long?, rule: String?): Long? {
        if (currentMillis == null || rule.isNullOrBlank()) return null
        val zone = ZoneId.systemDefault()
        val zdt = Instant.ofEpochMilli(currentMillis).atZone(zone)

        return when (rule) {
            "codziennie" -> zdt.plusDays(1).toInstant().toEpochMilli()
            "co tydzień" -> zdt.plusWeeks(1).toInstant().toEpochMilli()
            "co miesiąc" -> zdt.plusMonths(1).toInstant().toEpochMilli()
            else -> {
                // format: every:count:unit (unit: days|weeks|months)
                if (rule.startsWith("every:")) {
                    val parts = rule.split(":")
                    if (parts.size >= 3) {
                        val count = parts[1].toLongOrNull() ?: return null
                        when (parts[2]) {
                            "days" -> zdt.plusDays(count).toInstant().toEpochMilli()
                            "weeks" -> zdt.plusWeeks(count).toInstant().toEpochMilli()
                            "months" -> zdt.plusMonths(count).toInstant().toEpochMilli()
                            else -> null
                        }
                    } else null
                } else {
                    null
                }
            }
        }
    }

    /**
     * Oznacz zadanie jako wykonane. Jeśli ma regułę powtarzalności — utwórz kolejne wystąpienie.
     *
     * Zmiana: gdy brak dueAt, traktujemy createdAt jako początek serii (base) i dla nowego wystąpienia
     * zawsze ustawiamy dueAt = nextBase (żeby nowe wystąpienie miało termin i było widoczne).
     */
    fun completeTask(task: Task) {
        // guard: jeśli już DONE – nic nie rób
        if (task.status == TaskStatus.DONE) return

        viewModelScope.launch {
            // oznacz istniejące zadanie jako DONE
            val done = task.copy(status = TaskStatus.DONE)
            repo.update(done)

            // usuń powiązane schedulery/geofence dla tego (wykonanego) zadania
            context?.let { ctx ->
                ReminderScheduler.cancelReminder(ctx, done.id)
                GeofenceManager.removeGeofenceForTask(ctx, done.id)
            }

            // jeśli jest reguła powtarzalności, utwórz kolejne wystąpienie
            val rule = task.recurringRule
            if (!rule.isNullOrBlank()) {
                // wybierz bazę do obliczeń:
                // preferuj dueAt, jeśli brak -> reminderTimeMillis, jeśli brak -> createdAt
                val baseMillis = task.dueAt ?: task.reminderTimeMillis ?: task.createdAt
                val nextBase = computeNextMillis(baseMillis, rule)
                if (nextBase != null) {
                    // oblicz przesunięcie przypomnienia względem wybranej bazy (jeśli istniało)
                    val reminderOffset: Long? = task.reminderTimeMillis?.let { it - baseMillis }

                    val nextReminder = if (reminderOffset != null) nextBase + reminderOffset else null

                    val newTask = task.copy(
                        id = 0L,
                        status = TaskStatus.PENDING,
                        createdAt = System.currentTimeMillis(),
                        // ustawiamy dueAt zawsze na nextBase (tak, nawet jeśli oryginał nie miał dueAt),
                        // żeby nowe wystąpienie miało termin i było widoczne w UI.
                        dueAt = nextBase,
                        reminderTimeMillis = nextReminder,
                        // zachowujemy recurringRule aby kolejne też się powtarzały
                        recurringRule = task.recurringRule
                    )

                    // użyj istniejącej create(...) żeby poprawnie dodać przypomnienia i geofence
                    create(newTask) { /* id asynchronicznie */ }
                }
            }
        }
    }

    // Przywróć zadanie do stanu oczekującego
    fun reopenTask(task: Task) {
        if (task.status == TaskStatus.PENDING) return
        viewModelScope.launch {
            val reopened = task.copy(status = TaskStatus.PENDING)
            update(reopened)
        }
    }

    // cycle priority: LOW -> MEDIUM -> HIGH -> LOW
    fun cyclePriority(task: Task) {
        val next = when (task.priority) {
            Priority.LOW -> Priority.MEDIUM
            Priority.MEDIUM -> Priority.HIGH
            Priority.HIGH -> Priority.LOW
        }
        update(task.copy(priority = next))
    }

    // set explicit priority
    fun setPriority(task: Task, p: Priority) {
        update(task.copy(priority = p))
    }

    class Factory(private val repo: TaskRepository, private val context: Context? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TaskViewModel(repo, context) as T
        }
    }
}