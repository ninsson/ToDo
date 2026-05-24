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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val db = DatabaseProvider.get(applicationContext)
        val repo = TaskRepository(db.taskDao())

        setContent {
            ToDoTheme {
                val vm: TaskViewModel = viewModel(factory = TaskViewModel.Factory(repo, applicationContext))
                MainNavHost(viewModel = vm)
            }
        }
    }
}