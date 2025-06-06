variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "region" {
  description = "The GCP region to deploy resources"
  type        = string
  default     = "asia-northeast1"  # As specified in your example
}

variable "container_image" {
  description = "The container image URL for the Todo API"
  type        = string
  default     = "gcr.io/your-project/todo-api:latest"  # Replace with your actual image
}

variable "db_password" {
  description = "Password for the database user"
  type        = string
  sensitive   = true
}
