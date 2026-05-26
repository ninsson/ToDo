package com.example.todo

import androidx.compose.runtime.Composable
import androidx.compose.material3.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.todo.viewmodel.TaskViewModel
import com.example.todo.screens.TaskListScreen
import com.example.todo.screens.TaskEditScreen
import com.example.todo.screens.TaskDetailScreen
import com.example.todo.screens.SettingsScreen

@Composable
fun MainNavHost(navController: NavHostController, viewModel: TaskViewModel, startDestination: String = "list") {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("list") {
            TaskListScreen(navController = navController, viewModel = viewModel)
        }
        composable("create") {
            TaskEditScreen(navController = navController, viewModel = viewModel, taskId = null)
        }
        composable("edit/{id}") { backStack ->
            val id = backStack.arguments?.getString("id")?.toLongOrNull()
            TaskEditScreen(navController = navController, viewModel = viewModel, taskId = id)
        }
        composable("details/{id}") { backStack ->
            val id = backStack.arguments?.getString("id")?.toLongOrNull()
            if (id != null) TaskDetailScreen(navController = navController, viewModel = viewModel, taskId = id)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
    }
}