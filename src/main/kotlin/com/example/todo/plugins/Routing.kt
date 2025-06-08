package com.example.todo.plugins

import com.example.todo.models.Task
import com.example.todo.repository.TaskRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(taskRepository: TaskRepository) {
    routing {
        // Health check endpoint
        get("/") {
            call.respond(mapOf("status" to "UP", "message" to "Todo API is running"))
        }
        
        get("/health") {
            call.respond(mapOf("status" to "UP"))
        }
        
        // Task routes
        route("/tasks") {
            // Get all tasks
            get {
                val tasks = taskRepository.getAllTasks()
                call.respond(tasks)
            }
            
            // Get a specific task
            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@get call.respondText("Invalid ID", status = HttpStatusCode.BadRequest)
                
                val task = taskRepository.getTask(id)
                    ?: return@get call.respondText("Task not found", status = HttpStatusCode.NotFound)
                
                call.respond(task)
            }
            
            // Create a new task
            post {
                val task = call.receive<Task>()
                val createdTask = taskRepository.addTask(task)
                call.respond(HttpStatusCode.Created, createdTask)
            }
            
            // Update a task
            put("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@put call.respondText("Invalid ID", status = HttpStatusCode.BadRequest)
                
                val task = call.receive<Task>()
                val updated = taskRepository.updateTask(id, task)
                
                if (updated) {
                    val updatedTask = taskRepository.getTask(id)
                    if (updatedTask != null) {
                        call.respond(updatedTask)
                    } else {
                        call.respondText("Task not found after update", status = HttpStatusCode.InternalServerError)
                    }
                } else {
                    call.respondText("Task not found", status = HttpStatusCode.NotFound)
                }
            }
            
            // Delete a task
            delete("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respondText("Invalid ID", status = HttpStatusCode.BadRequest)
                
                val deleted = taskRepository.deleteTask(id)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respondText("Task not found", status = HttpStatusCode.NotFound)
                }
            }
        }
    }
}
