package com.example.todo.repository

import com.example.todo.database.DatabaseFactory.dbQuery
import com.example.todo.models.Task
import com.example.todo.models.Tasks
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TaskRepository {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    
    suspend fun getAllTasks(): List<Task> = dbQuery {
        Tasks.selectAll()
            .map { resultRowToTask(it) }
    }
    
    suspend fun getTask(id: Int): Task? = dbQuery {
        Tasks.select { Tasks.id eq id }
            .map { resultRowToTask(it) }
            .singleOrNull()
    }
    
    suspend fun addTask(task: Task): Task {
        var key = 0
        dbQuery {
            key = (Tasks.insert {
                it[title] = task.title
                it[description] = task.description
                it[completed] = task.completed
            } get Tasks.id)
        }
        return getTask(key) ?: throw IllegalStateException("Failed to create task")
    }
    
    suspend fun updateTask(id: Int, task: Task): Boolean = dbQuery {
        Tasks.update({ Tasks.id eq id }) {
            it[title] = task.title
            it[description] = task.description
            it[completed] = task.completed
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }
    
    suspend fun deleteTask(id: Int): Boolean = dbQuery {
        Tasks.deleteWhere { Tasks.id eq id } > 0
    }
    
    private fun resultRowToTask(row: ResultRow): Task =
        Task(
            id = row[Tasks.id],
            title = row[Tasks.title],
            description = row[Tasks.description],
            completed = row[Tasks.completed],
            createdAt = row[Tasks.createdAt].format(formatter),
            updatedAt = row[Tasks.updatedAt].format(formatter)
        )
}
