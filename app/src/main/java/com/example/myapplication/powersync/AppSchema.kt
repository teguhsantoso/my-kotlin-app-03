package com.example.myapplication.powersync

import com.powersync.db.schema.Column
import com.powersync.db.schema.Schema
import com.powersync.db.schema.Table


val AppSchema = Schema(
    tables = listOf(
        Table(
            name = "tasks",
            columns = listOf(
                Column.text("title"),
                Column.integer("isCompleted")
            )
    ),
        Table(
            name = "pending_operations",
            columns = listOf(
                Column.text("taskId"),
                Column.text("oepration"),
                Column.text("title"),
                Column.integer("isCompleted")
            )
        )
    )
)