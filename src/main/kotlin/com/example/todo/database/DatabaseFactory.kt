package com.example.todo.database

import com.example.todo.models.Tasks
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object DatabaseFactory {
    fun init() {
        try {
            println("🚀 Initializing database connection...")
            
            val dataSource = hikari()
            Database.connect(dataSource)
            
            // Test connection immediately
            dataSource.connection.use { conn ->
                println("✅ Successfully connected to ${conn.metaData.databaseProductName} ${conn.metaData.databaseProductVersion}")
                println("📊 Database: ${conn.catalog}")
            }
            
            // Create tables if they don't exist
            transaction {
                SchemaUtils.create(Tasks)
                println("🆕 Created 'Tasks' table successfully")
            }
            
            println("🎉 Database initialization complete!")
        } catch (e: Exception) {
            println("❌ FATAL: Database initialization failed")
            println("Error details: ${e.message}")
            e.printStackTrace()
            throw e // Fail fast during application startup
        }
    }
    
    private fun hikari(): HikariDataSource {
        // Get database connection info from environment variables with defaults
        val dbHost = System.getenv("DB_HOST") ?: "localhost"
        val dbPort = System.getenv("DB_PORT") ?: "5432"
        val dbName = System.getenv("DB_NAME") ?: "todo_db"
        val dbUser = System.getenv("DB_USER") ?: "todo_user"
        val dbPassword = System.getenv("DB_PASSWORD") ?: "todo_password"
        
        val jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"
        
        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = dbUser
            this.password = dbPassword
            
            driverClassName = "org.postgresql.Driver"
            
            // Pool settings
            maximumPoolSize = 5
            minimumIdle = 1
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            
            // Timeouts
            connectionTimeout = 30000 // 30 seconds
            validationTimeout = 5000  // 5 seconds
            idleTimeout = 600000      // 10 minutes
            maxLifetime = 1800000     // 30 minutes
            
            // Monitoring
            poolName = "TodoAppPool-${UUID.randomUUID().toString().take(4)}"
            leakDetectionThreshold = 60000 // 1 minute
            initializationFailTimeout = 10000 // Fail fast if can't connect
        }
        
        println("🔧 HikariCP Configuration:")
        println(" - JDBC URL: ${config.jdbcUrl}")
        println(" - Username: ${config.username}")
        println(" - Pool Size: ${config.maximumPoolSize}")
        
        return HikariDataSource(config)
    }
    
    suspend fun <T> dbQuery(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction { block() }
        }
}
