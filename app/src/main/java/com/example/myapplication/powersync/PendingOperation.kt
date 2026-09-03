package com.example.myapplication.powersync

import com.powersync.db.schema.Column
import com.powersync.db.schema.Table

data class PendingOperation(
    val id: String,
    val taskId: String,
    val operation: String,
    val title: String?,
    val isCompleted: Boolean?
) {
    companion object {
        val table = Table(
            name = "pending_operations",
            columns = listOf(
                Column.text("taskId"),
                Column.text("operation"),
                Column.text("title"),
                Column.integer("isCompleted")
            )
        )
    }
}
