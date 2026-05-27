package com.example.todo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.todo.ui.theme.ToDoTheme
import com.example.todo.viewmodel.TaskViewModel
import com.example.todo.repo.TaskRepository
import com.example.todo.settings.SettingsRepository
import com.example.todo.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Główna aktywność aplikacji.
 *
 * Odpowiada za:
 * - inicjalizację bazy i repozytorium,
 * - wczytanie ustawień motywu,
 * - start nawigacji i obsługę intentów (np. z widgetu lub powiadomień).
 */
class MainActivity : ComponentActivity() {

    /**
     * Strumień intencji używany do obsługi wejść z zewnątrz (widget/powiadomienia).
     */
    val navIntentFlow = MutableStateFlow<Intent?>(null)

    /**
     * Inicjalizacja UI Compose i konfiguracja startowego ekranu na podstawie intentu.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val db = DatabaseProvider.get(applicationContext)
        val repo = TaskRepository(db.taskDao())

        // ustawiam początkowy intent (może być null)
        navIntentFlow.value = intent

        setContent {
            val settingsRepo = remember { SettingsRepository(applicationContext) }
            val themeMode by settingsRepo.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            ToDoTheme(darkTheme = darkTheme) {
                val nav = rememberNavController()

                // pobieramy aktualny intent (może być null)
                val startIntent by navIntentFlow.collectAsState()

                // jeśli startIntent ma extra open_create = true -> startujemy bezpośrednio na "create"
                val initialDestination = remember(startIntent) {
                    val i = startIntent
                    if (i != null && i.getBooleanExtra("open_create", false)) {
                        "create"
                    } else {
                        "list"
                    }
                }

                val vm: TaskViewModel =
                    viewModel(factory = TaskViewModel.Factory(repo, applicationContext))
                MainNavHost(
                    navController = nav,
                    viewModel = vm,
                    startDestination = initialDestination
                )

                // dalej reagujemy na przychodzące intenty (np. otwarcie zadania szczegółów)
                LaunchedEffect(startIntent) {
                    val i = startIntent ?: return@LaunchedEffect
                    val openTaskId = i.getLongExtra("open_task_id", -1L)
                    val openCreate = i.getBooleanExtra("open_create", false)

                    when {
                        openTaskId > 0 -> {
                            nav.navigate("details/$openTaskId") {
                                launchSingleTop = true
                            }
                        }

                        openCreate -> {
                            // Jeśli już ustawiliśmy startDestination na "create", nic więcej nie trzeba robić.
                        }
                    }

                    navIntentFlow.value = null
                }
            }
        }
    }

    /**
     * Obsługa nowych intentów (np. gdy użytkownik kliknie powiadomienie).
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent ?: return

        // ustaw nowy intent dla activity
        setIntent(intent)

        lifecycleScope.launch {
            navIntentFlow.value = intent
        }
    }
}