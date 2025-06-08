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
            
            // Determine connection mode
            val connectionMode = determineConnectionMode()
            println("🔌 Connection mode: $connectionMode")
            
            val dataSource = hikari(connectionMode)
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
    
    private fun determineConnectionMode(): ConnectionMode {
        return when {
            System.getenv("JDBC_URL") != null -> ConnectionMode.GCP_CLOUD_SQL
            else -> ConnectionMode.DIRECT_CONNECTION
        }
    }
    
    private fun hikari(connectionMode: ConnectionMode): HikariDataSource {
        val jdbcUrl = when (connectionMode) {
            ConnectionMode.GCP_CLOUD_SQL -> {
                System.getenv("JDBC_URL") ?: throw Exception("JDBC_URL environment variable is required for GCP mode")
            }
            ConnectionMode.DIRECT_CONNECTION -> {
                val dbHost = System.getenv("DB_HOST") ?: "localhost"
                val dbPort = System.getenv("DB_PORT") ?: "5432"
                val dbName = System.getenv("DB_NAME") ?: "postgres_db"
                "jdbc:postgresql://$dbHost:$dbPort/$dbName"
            }
        }
        
        val dbUser = System.getenv("DB_USER") ?: "postgres"
        val dbPassword = System.getenv("DB_PASSWORD") ?: "postgres_password"
        
        println("🔧 Database Configuration:")
        println(" - JDBC URL: $jdbcUrl")
        println(" - Username: $dbUser")
        
        val config = HikariConfig()
        
        config.apply {
            this.jdbcUrl = jdbcUrl
            this.username = dbUser
            this.password = dbPassword
            this.driverClassName = "org.postgresql.Driver"
            
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
        
        println(" - Pool Size: ${config.maximumPoolSize}")
        println(" - Environment: $connectionMode")
        
        return HikariDataSource(config)
    }
    
    fun shutdown() {
        // No resources to close in direct connection mode
    }
    
    suspend fun <T> dbQuery(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction { block() }
        }
}

enum class ConnectionMode {
    DIRECT_CONNECTION,
    GCP_CLOUD_SQL
}
