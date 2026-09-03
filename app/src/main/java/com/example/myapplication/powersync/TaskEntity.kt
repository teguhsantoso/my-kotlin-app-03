package com.example.myapplication.powersync

import com.powersync.db.schema.Column
import com.powersync.db.schema.Table

data class TaskEntity(
    val id: String,
    val title: String,
    val isCompleted: Boolean
) {
    companion object {
        val table = Table(
            name = "tasks",
            columns = listOf(
                Column.text("title"),
                Column.integer("isCompleted")
            )
        )
    }
}
