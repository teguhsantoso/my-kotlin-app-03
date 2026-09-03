package com.example.myapplication.powersync

import com.powersync.db.schema.Schema

val AppSchema = Schema(
    tables = listOf(
        TaskEntity.table,
        PendingOperation.table
    )
)
