package com.example.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge // DODANY IMPORT
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.todo.ui.theme.ToDoTheme
import com.example.todo.MainNavHost
import com.example.todo.viewmodel.TaskViewModel
import com.example.todo.data.AppDatabase
import com.example.todo.repo.TaskRepository
import androidx.room.Room

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "todo-db"
        ).fallbackToDestructiveMigration().build()

        val repo = TaskRepository(db.taskDao())

        setContent {
            ToDoTheme {
                // Przekazujemy applicationContext do factory, aby ViewModel mógł planować przypomnienia
                val vm: TaskViewModel = viewModel(factory = TaskViewModel.Factory(repo, applicationContext))
                MainNavHost(viewModel = vm)
            }
        }
    }
}