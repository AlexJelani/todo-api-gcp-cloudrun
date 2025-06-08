package com.example.todo

import com.example.todo.database.DatabaseFactory
import com.example.todo.plugins.configureRouting
import com.example.todo.plugins.configureSerialization
import com.example.todo.repository.TaskRepository
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import kotlin.system.exitProcess

fun main() {
    // Get port from environment variable PORT or default to 8080
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    println("🚀 Starting server on port $port")
    
    val server = embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }
    
    // Add shutdown hook to close resources properly
    Runtime.getRuntime().addShutdownHook(Thread {
        println("🛑 Shutting down server...")
        server.stop(1000, 2000)
        DatabaseFactory.shutdown()
        println("👋 Server shutdown complete")
    })
    
    try {
        server.start(wait = true)
    } catch (e: Exception) {
        println("❌ Failed to start server: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
    }
}

fun Application.module() {
    // Initialize database
    DatabaseFactory.init()
    
    // Create repository
    val taskRepository = TaskRepository()
    
    // Configure CORS
    install(CORS) {
        anyHost()
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
    }
    
    // Configure plugins
    configureSerialization()
    configureRouting(taskRepository)
}
