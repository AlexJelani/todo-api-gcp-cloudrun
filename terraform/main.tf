provider "google" {
  project = var.project_id
  region  = var.region
}

# Enable required APIs
resource "google_project_service" "cloud_run_api" {
  service = "run.googleapis.com"
  disable_on_destroy = false
}

resource "google_project_service" "artifact_registry_api" {
  service = "artifactregistry.googleapis.com"
  disable_on_destroy = false
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
        
        # Direct connection to OCI PostgreSQL
        env {
          name  = "DB_HOST"
          value = var.db_host
        }
        
        env {
          name  = "DB_PORT"
          value = var.db_port
        }
        
        env {
          name  = "DB_NAME"
          value = var.db_name
        }
        
        env {
          name  = "DB_USER"
          value = var.db_user
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
            path = "/health"
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
    google_project_service.cloud_run_api
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
