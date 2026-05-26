package com.example.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todo.ui.theme.ToDoTheme
import com.example.todo.MainNavHost
import com.example.todo.viewmodel.TaskViewModel
import com.example.todo.repo.TaskRepository
import com.example.todo.DatabaseProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val db = DatabaseProvider.get(applicationContext)
        val repo = TaskRepository(db.taskDao())

        setContent {
            ToDoTheme {
                val nav = rememberNavController()
                val vm: TaskViewModel = viewModel(factory = TaskViewModel.Factory(repo, applicationContext))
                MainNavHost(navController = nav, viewModel = vm)

                // jeśli aktywność została uruchomiona z extra open_task_id, przejdź do szczegółów
                val startIntent = intent
                LaunchedEffect(startIntent) {
                    val id = startIntent?.getLongExtra("open_task_id", -1L) ?: -1L
                    if (id > 0) {
                        // użyj launchSingleTop żeby nie duplikować stosu
                        nav.navigate("details/$id") {
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }
}