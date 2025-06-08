variable "project_id" {
  description = "The GCP project ID"
  type        = string
}

variable "region" {
  description = "The GCP region to deploy resources"
  type        = string
  default     = "asia-northeast1"
}

variable "container_image" {
  description = "The container image URL for the Todo API"
  type        = string
}

variable "db_password" {
  description = "Password for the database user"
  type        = string
  sensitive   = true
}

variable "db_host" {
  description = "PostgreSQL host on OCI"
  type        = string
  default     = "131.186.56.105"
}

variable "db_port" {
  description = "PostgreSQL port"
  type        = string
  default     = "5432"
}

variable "db_name" {
  description = "PostgreSQL database name"
  type        = string
  default     = "postgres_db"
}

variable "db_user" {
  description = "PostgreSQL username"
  type        = string
  default     = "postgres"
}
