package com.example.myapplication.data

import android.content.Context
import android.util.Log.d
import android.util.Log.e
import com.example.myapplication.powersync.PowerSyncProvider
import com.example.myapplication.remote.ApiClient
import com.example.myapplication.remote.Task
import com.powersync.PowerSyncDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class TaskRepository(context: Context) {

    private companion object {
        const val TAG = "TaskRepository"
        const val OPERATION_UPSERT = "UPSERT"
        const val OPERATION_UPDATE = "UPDATE"
        const val OPERATION_DELETE = "DELETE"
        const val FIELD_TITLE = "title"
        const val FIELD_IS_COMPLETED = "isCompleted"
    }

    private val database: PowerSyncDatabase = PowerSyncProvider.getDatabase(context)
    private val taskAdapter = ApiClient.taskAdapter

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    suspend fun refreshTasks() {
        refreshLocalState()
        syncPendingOperations()

        if (hasPendingOperations()) {
            d(TAG, "Pending operations still exist. Skip remote overwrite.")
            refreshLocalState()
            return
        }

        try {
            val response = taskAdapter.getAllTasks()
            if (response.isSuccessful) {
                replaceLocalTasks(response.body().orEmpty())
                refreshLocalState()
            }
        } catch (_: Exception) {
            refreshLocalState()
        }
    }

    suspend fun addTask(title: String) {
        val task = Task(
            id = UUID.randomUUID().toString(),
            title = title,
            isCompleted = false
        )

        insertTaskLocally(task)
        val pendingOperationId = savePendingOperation(task, OPERATION_UPSERT)
        refreshLocalState()

        try {
            val response = taskAdapter.upsertTask(task)
            if (response.isSuccessful) {
                deletePendingOperation(pendingOperationId)
            } else {
                e(
                    TAG,
                    "Failed to upload task. HTTP ${response.code()}: ${response.errorBody()?.string()}"
                )
            }
        } catch (exception: Exception) {
            e(TAG, "Failed to upload task", exception)
        }
    }

    suspend fun toggleTask(task: Task) {
        val updatedTask = task.copy(
            isCompleted = !task.isCompleted
        )

        updateTaskCompletionLocally(updatedTask)
        refreshLocalState()

        try {
            val response = taskAdapter.updateTask(
                id = updatedTask.id,
                fields = mapOf(
                    FIELD_IS_COMPLETED to updatedTask.isCompleted
                )
            )

            if (!response.isSuccessful) {
                savePendingOperation(updatedTask, OPERATION_UPDATE)
            }
        } catch (exception: Exception) {
            e(TAG, "Failed to update task", exception)
            savePendingOperation(updatedTask, OPERATION_UPDATE)
        }
    }

    suspend fun deleteTask(task: Task) {
        deleteTaskLocally(task.id)
        refreshLocalState()

        try {
            val response = taskAdapter.deleteTask(task.id)
            if (!response.isSuccessful) {
                savePendingOperation(task, OPERATION_DELETE)
            }
        } catch (exception: Exception) {
            e(TAG, "Failed to delete task", exception)
            savePendingOperation(task, OPERATION_DELETE)
        }
    }

    private suspend fun refreshLocalState() {
        _tasks.value = getLocalTasks()
    }

    private suspend fun getLocalTasks(): List<Task> {
        return database.getAll(
            """
            SELECT id, title, isCompleted
            FROM tasks
            ORDER BY title
            """.trimIndent()
        ) { cursor ->
            Task(
                id = cursor.getString(0).orEmpty(),
                title = cursor.getString(1).orEmpty(),
                isCompleted = cursor.getLong(2).toBoolean()
            )
        }
    }

    private suspend fun replaceLocalTasks(tasks: List<Task>) {
        database.execute("DELETE FROM tasks")

        tasks.forEach { task ->
            insertTaskLocally(task)
        }
    }

    private suspend fun insertTaskLocally(task: Task) {
        database.execute(
            """
            INSERT INTO tasks(id, title, isCompleted)
            VALUES (?, ?, ?)
            """.trimIndent(),
            listOf(
                task.id,
                task.title,
                task.isCompleted.toDatabaseValue()
            )
        )
    }

    private suspend fun updateTaskCompletionLocally(task: Task) {
        database.execute(
            """
            UPDATE tasks
            SET isCompleted = ?
            WHERE id = ?
            """.trimIndent(),
            listOf(
                task.isCompleted.toDatabaseValue(),
                task.id
            )
        )
    }

    private suspend fun deleteTaskLocally(taskId: String) {
        database.execute(
            """
            DELETE FROM tasks
            WHERE id = ?
            """.trimIndent(),
            listOf(taskId)
        )
    }

    private suspend fun hasPendingOperations(): Boolean {
        val pendingCount = database.getAll(
            """
            SELECT COUNT(*)
            FROM pending_operations
            """.trimIndent()
        ) { cursor ->
            cursor.getLong(0) ?: 0L
        }.firstOrNull() ?: 0L

        return pendingCount > 0L
    }

    private suspend fun savePendingOperation(
        task: Task,
        operation: String
    ): String {
        val pendingOperationId = UUID.randomUUID().toString()

        database.execute(
            """
            INSERT INTO pending_operations(id, taskId, operation, title, isCompleted)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            listOf(
                pendingOperationId,
                task.id,
                operation,
                task.title,
                task.isCompleted.toDatabaseValue()
            )
        )

        return pendingOperationId
    }

    private suspend fun getPendingOperations(): List<PendingOperation> {
        return database.getAll(
            """
            SELECT id, taskId, operation, title, isCompleted
            FROM pending_operations
            ORDER BY id
            """.trimIndent()
        ) { cursor ->
            PendingOperation(
                id = cursor.getString(0).orEmpty(),
                taskId = cursor.getString(1).orEmpty(),
                operation = cursor.getString(2).orEmpty(),
                title = cursor.getString(3).orEmpty(),
                isCompleted = cursor.getLong(4).toBoolean()
            )
        }
    }

    private suspend fun syncPendingOperations() {
        getPendingOperations().forEach { operation ->
            try {
                syncPendingOperation(operation)
            } catch (_: Exception) {
                return
            }
        }
    }

    private suspend fun syncPendingOperation(operation: PendingOperation) {
        val responseIsSuccessful = when (operation.operation) {
            OPERATION_UPSERT -> {
                taskAdapter.upsertTask(operation.toTask()).isSuccessful
            }

            OPERATION_UPDATE -> {
                taskAdapter.updateTask(
                    id = operation.taskId,
                    fields = mapOf(
                        FIELD_TITLE to operation.title,
                        FIELD_IS_COMPLETED to operation.isCompleted
                    )
                ).isSuccessful
            }

            OPERATION_DELETE -> {
                taskAdapter.deleteTask(operation.taskId).isSuccessful
            }

            else -> false
        }

        if (responseIsSuccessful) {
            deletePendingOperation(operation.id)
        }
    }

    private suspend fun deletePendingOperation(id: String) {
        database.execute(
            """
            DELETE FROM pending_operations
            WHERE id = ?
            """.trimIndent(),
            listOf(id)
        )
    }

    private fun Boolean.toDatabaseValue(): Int {
        return if (this) 1 else 0
    }

    private fun Long?.toBoolean(): Boolean {
        return this == 1L
    }

    private fun PendingOperation.toTask(): Task {
        return Task(
            id = taskId,
            title = title,
            isCompleted = isCompleted
        )
    }

    private data class PendingOperation(
        val id: String,
        val taskId: String,
        val operation: String,
        val title: String,
        val isCompleted: Boolean
    )
}