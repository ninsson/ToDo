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

/**
 * Główny host nawigacji aplikacji.
 * * Odpowiada za definiowanie mapy ekranów oraz bezpieczne przekazywanie parametrów
 * pomiędzy nimi. Jest to centralny punkt kontroli przepływu użytkownika.
 *
 * @param navController Kontroler nawigacji, który zarządza stosem ekranów.
 * @param viewModel ViewModel współdzielony przez wszystkie ekrany w celu dostępu do danych.
 * @param startDestination Trasa startowa aplikacji (domyślnie "list").
 */
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