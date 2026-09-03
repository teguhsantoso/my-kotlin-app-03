package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.TaskRepository
import com.example.myapplication.remote.Task
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: TaskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repository = TaskRepository(applicationContext)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TaskScreen(
                        repository = repository,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}


@Composable
fun TaskScreen(
    repository: TaskRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val tasks by repository.tasks.collectAsState()
    var title by remember { mutableStateOf("") }

    // LaunchedEffect(Unit) {
    //      Coroutine-Code hier
    // }
    // bedeutet:
    // Starte diesen Code einmal, wenn das Composable angezeigt wird.
    //
    // LaunchedEffect(someValue) {
    //      Coroutine-Code hier
    //}
    // bedeutet:
    // Starte diesen Code beim ersten Anzeigen und jedes Mal neu, wenn sich someValue ändert.
    LaunchedEffect(Unit) {
        repository.refreshTasks()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.weight(1f),
                label = {
                    Text("New task")
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        scope.launch {
                            repository.addTask(title.trim())
                            title = ""
                        }
                    }
                }
            ) {
                Text("Add")
            }
        }

        Button(
            onClick = {
                scope.launch {
                    repository.refreshTasks()
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Sync")
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            items(tasks) { task ->
                TaskRow(
                    task = task,
                    onToggle = {
                        scope.launch {
                            repository.toggleTask(task)
                        }
                    },
                    onDelete = {
                        scope.launch {
                            repository.deleteTask(task)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = {
                onToggle()
            }
        )

        Text(
            text = task.title,
            modifier = Modifier.weight(1f)
        )

        Button(
            onClick = onDelete
        ) {
            Text("Delete")
        }
    }
}