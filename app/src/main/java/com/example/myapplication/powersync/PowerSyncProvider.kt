package com.example.myapplication.powersync

import android.content.Context
import com.powersync.DatabaseDriverFactory
import com.powersync.PowerSyncDatabase

object PowerSyncProvider {

    const val BACKEND_BASE_URL = "http://192.168.178.61:8080/"

    private var database: PowerSyncDatabase? = null

    fun getDatabase(context: Context): PowerSyncDatabase {
        return database ?: PowerSyncDatabase(
            factory = DatabaseDriverFactory(context.applicationContext),
            schema = AppSchema,
            dbFilename = "tasks.db"
        ).also {
            database = it
        }
    }
}