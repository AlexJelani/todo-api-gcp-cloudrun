package com.example.todo.database

import com.example.todo.models.Tasks
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import java.util.Properties

object DatabaseFactory {
    private var sshSession: Session? = null
    private var localPort: Int = 0
    
    fun init() {
        try {
            println("🚀 Initializing database connection...")
            
            // Determine connection mode
            val connectionMode = determineConnectionMode()
            println("🔌 Connection mode: $connectionMode")
            
            // Setup SSH tunnel if needed
            if (connectionMode == ConnectionMode.REMOTE_SSH) {
                setupSshTunnel()
            }
            
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
            System.getenv("SSH_HOST") != null -> ConnectionMode.REMOTE_SSH
            else -> ConnectionMode.LOCAL_DOCKER
        }
    }
    
    private fun setupSshTunnel() {
        try {
            println("🔒 Setting up SSH tunnel...")
            
            val sshHost = System.getenv("SSH_HOST") ?: throw Exception("SSH_HOST environment variable is required")
            val sshUser = System.getenv("SSH_USER") ?: throw Exception("SSH_USER environment variable is required")
            val sshKeyPath = System.getenv("SSH_KEY_PATH")?.replace("~", System.getProperty("user.home")) 
                ?: throw Exception("SSH_KEY_PATH environment variable is required")
            val remotePort = System.getenv("DB_PORT")?.toInt() ?: 5432
            
            // Find a free local port
            val socket = java.net.ServerSocket(0)
            localPort = socket.localPort
            socket.close()
            
            val jsch = JSch()
            jsch.addIdentity(sshKeyPath)
            
            sshSession = jsch.getSession(sshUser, sshHost, 22)
            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            sshSession!!.setConfig(config)
            sshSession!!.connect()
            
            // Forward local port to remote database port
            sshSession!!.setPortForwardingL(localPort, "localhost", remotePort)
            
            println("✅ SSH tunnel established: localhost:$localPort -> $sshHost:$remotePort")
        } catch (e: Exception) {
            println("❌ Failed to establish SSH tunnel")
            e.printStackTrace()
            throw e
        }
    }
    
    private fun hikari(connectionMode: ConnectionMode): HikariDataSource {
        val jdbcUrl = when (connectionMode) {
            ConnectionMode.GCP_CLOUD_SQL -> {
                System.getenv("JDBC_URL") ?: throw Exception("JDBC_URL environment variable is required for GCP mode")
            }
            ConnectionMode.REMOTE_SSH -> {
                // Use the local forwarded port for the database connection
                val dbName = System.getenv("DB_NAME") ?: "postgres_db"
                "jdbc:postgresql://localhost:$localPort/$dbName"
            }
            ConnectionMode.LOCAL_DOCKER -> {
                val dbHost = System.getenv("DB_HOST") ?: "localhost"
                val dbPort = System.getenv("DB_PORT") ?: "5432"
                val dbName = System.getenv("DB_NAME") ?: "todo_db"
                "jdbc:postgresql://$dbHost:$dbPort/$dbName"
            }
        }
        
        val dbUser = System.getenv("DB_USER") ?: "postgres"
        val dbPassword = System.getenv("DB_PASSWORD") ?: "postgres"
        
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
        sshSession?.disconnect()
        println("🔌 SSH tunnel closed")
    }
    
    suspend fun <T> dbQuery(block: () -> T): T =
        withContext(Dispatchers.IO) {
            transaction { block() }
        }
}

enum class ConnectionMode {
    LOCAL_DOCKER,
    REMOTE_SSH,
    GCP_CLOUD_SQL
}
