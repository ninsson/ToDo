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

        // Włącza tryb Edge-to-Edge w Material 3 – aplikacja zlewa się z paskiem statusu
        enableEdgeToEdge()

        // Prosty builder DB (możesz zainicjalizować przez DI)
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "todo-db"
        ).fallbackToDestructiveMigration().build()

        val repo = TaskRepository(db.taskDao())

        setContent {
            ToDoTheme {
                val vm: TaskViewModel = viewModel(factory = TaskViewModel.Factory(repo))
                MainNavHost(viewModel = vm)
            }
        }
    }
}