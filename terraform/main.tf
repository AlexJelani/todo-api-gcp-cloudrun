provider "google" {
  project = var.project_id
  region  = var.region
}

# Enable required APIs
resource "google_project_service" "cloud_run_api" {
  service = "run.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "cloud_sql_api" {
  service = "sqladmin.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "artifact_registry_api" {
  service = "artifactregistry.googleapis.com"
  disable_on_destroy = false
}

# Create PostgreSQL instance
resource "google_sql_database_instance" "todo_db_instance" {
  name             = "todo-db-instance"
  database_version = "POSTGRES_13"
  region           = var.region
  
  settings {
    tier = "db-f1-micro"  # Smallest instance, good for development
    
    backup_configuration {
      enabled = true
      start_time = "02:00"  # 2 AM UTC
    }
    
    ip_configuration {
      ipv4_enabled = true
      authorized_networks {
        name  = "all"
        value = "0.0.0.0/0"  # For development only, restrict in production
      }
    }
  }
  
  deletion_protection = false  # Set to true for production
}

# Create database
resource "google_sql_database" "todo_db" {
  name     = "todo_db"
  instance = google_sql_database_instance.todo_db_instance.name
}

# Create database user
resource "google_sql_user" "todo_db_user" {
  name     = "todo_user"
  instance = google_sql_database_instance.todo_db_instance.name
  password = var.db_password
}

# Create Cloud Run service
resource "google_cloud_run_service" "todo_api" {
  name     = "todo-api"
  location = var.region
  
  template {
    metadata {
      annotations = {
        "autoscaling.knative.dev/maxScale" = "10"
      }
    }
    
    spec {
      timeout_seconds = 600  # Increase timeout to 10 minutes
      
      containers {
        image = var.container_image
        
        env {
          name  = "JDBC_URL"
          value = "jdbc:postgresql:///${google_sql_database.todo_db.name}?cloudSqlInstance=${google_sql_database_instance.todo_db_instance.connection_name}&socketFactory=com.google.cloud.sql.postgres.SocketFactory"
        }
        
        env {
          name  = "DB_USER"
          value = google_sql_user.todo_db_user.name
        }
        
        env {
          name  = "DB_PASSWORD"
          value = var.db_password
        }
        
        env {
          name  = "PORT"
          value = "8080"
        }
        
        resources {
          limits = {
            cpu    = "1000m"
            memory = "512Mi"
          }
        }
        
        # Configure startup probe with extended timeout
        startup_probe {
          initial_delay_seconds = 10
          timeout_seconds       = 300  # 5 minutes timeout
          period_seconds        = 10   # Check every 10 seconds
          failure_threshold     = 30   # Allow up to 30 failures
          
          http_get {
            path = "/"
            port = 8080
          }
        }
      }
    }
  }
  
  traffic {
    percent         = 100
    latest_revision = true
  }
  
  depends_on = [
    google_project_service.cloud_run_api,
    google_sql_database.todo_db
  ]
}

# Make the Cloud Run service publicly accessible
resource "google_cloud_run_service_iam_member" "public_access" {
  service  = google_cloud_run_service.todo_api.name
  location = google_cloud_run_service.todo_api.location
  role     = "roles/run.invoker"
  member   = "allUsers"
}

# Output the service URL
output "service_url" {
  value = google_cloud_run_service.todo_api.status[0].url
}
