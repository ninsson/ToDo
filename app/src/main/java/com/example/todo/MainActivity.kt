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
 * Odpowiada za konfigurację środowiska (baza danych, ViewModel, nawigacja)
 * oraz obsługę zewnętrznych zdarzeń (np. otwieranie aplikacji przez powiadomienia).
 */
class MainActivity : ComponentActivity() {

    /**
     * Strumień intencji używany do obsługi wejść z zewnątrz.
     * Pozwala na asynchroniczne reagowanie na intencje wewnątrz UI (Compose).
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

                val startIntent by navIntentFlow.collectAsState()

                val initialDestination = remember {
                    if (intent.getBooleanExtra("open_create", false)) "create" else "list"
                }

                val vm: TaskViewModel =
                    viewModel(factory = TaskViewModel.Factory(repo, applicationContext))
                MainNavHost(
                    navController = nav,
                    viewModel = vm,
                    startDestination = initialDestination
                )

                // dalej reagujemy na przychodzące intenty
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
                            nav.navigate("create") {
                                launchSingleTop = true
                            }
                        }
                    }

                    navIntentFlow.value = null
                }
            }
        }
    }

    /**
     * Wywoływane, gdy system dostarcza nową intencję do działającej już aplikacji
     * (np. po kliknięciu powiadomienia, gdy aplikacja była w tle).
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