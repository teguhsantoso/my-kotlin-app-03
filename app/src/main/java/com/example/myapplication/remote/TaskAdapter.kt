package com.example.myapplication.remote

import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET


data class Task (
    val id: String,
    val title: String,
    val isCompleted: Boolean
)

interface TaskAdapter {

    @POST("api/tasks")
    suspend fun upsertTask(@Body task: Task): Response<Task>

    @PATCH("api/tasks/{id}")
    suspend fun updateTask(
        @Path("id") id: String,
        @Body fields: Map<String, @JvmSuppressWildcards Any?>
    ): Response<Unit>

    @DELETE("api/tasks/{id}")
    suspend fun deleteTask(
        @Path("id") id: String
    ): Response<Unit>

    @GET("api/tasks")
    suspend fun getAllTasks(): Response<List<Task>>
}