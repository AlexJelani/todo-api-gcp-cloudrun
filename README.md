# To-Do API with Kotlin and PostgreSQL

A simple RESTful API for managing to-do tasks, built with Kotlin, Ktor, and PostgreSQL.

## Project Structure

```
todo-api/
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── com/
│       │       └── example/
│       │           └── todo/
│       │               ├── database/
│       │               │   └── DatabaseFactory.kt
│       │               ├── models/
│       │               │   └── Task.kt
│       │               ├── plugins/
│       │               │   ├── Routing.kt
│       │               │   └── Serialization.kt
│       │               ├── repository/
│       │               │   └── TaskRepository.kt
│       │               └── Application.kt
│       └── resources/
│           ├── application.conf
│           └── logback.xml
├── terraform/
│   ├── main.tf
│   ├── variables.tf
│   └── terraform.tfvars.example
├── build.gradle.kts
├── gradle.properties
├── Dockerfile
└── docker-compose.yml
```

## Prerequisites

- JDK 17+
- Gradle
- Docker and Docker Compose (for containerized deployment)
- Google Cloud SDK (for GCP deployment)
- Terraform (for infrastructure as code)

## Running Locally

### 1. Start PostgreSQL

```bash
docker run --name todo-postgres \
  -e POSTGRES_USER=todo_user \
  -e POSTGRES_PASSWORD=todo_password \
  -e POSTGRES_DB=todo_db \
  -p 5432:5432 \
  -d postgres:13
```

### 2. Run the application

```bash
./gradlew run
```

The API will be available at http://localhost:8080

## Running with Docker Compose

```bash
docker-compose up -d
```

This will start both PostgreSQL and the application in containers.

## Deploying to Google Cloud Platform

The project includes Terraform configuration for deploying to Google Cloud Platform.

### 1. Build and push Docker image to Google Container Registry

```bash
# Build the image
docker build -t gcr.io/[YOUR_PROJECT_ID]/todo-api:latest .

# Configure Docker to use gcloud credentials
gcloud auth configure-docker

# Push the image
docker push gcr.io/[YOUR_PROJECT_ID]/todo-api:latest
```

### 2. Create terraform.tfvars file

Create a `terraform.tfvars` file in the terraform directory based on the example:

```
project_id     = "your-gcp-project-id"
region         = "asia-northeast1"
container_image = "gcr.io/your-gcp-project-id/todo-api:latest"
db_password    = "your-secure-password"
```

### 3. Apply Terraform configuration

```bash
cd terraform
terraform init
terraform plan
terraform apply
```

This will deploy:
- A PostgreSQL database in Cloud SQL
- A Cloud Run service running your To-Do API
- Necessary IAM permissions and API enablements

### 4. Access your deployed API

After deployment, Terraform will output the URL of your deployed API:

```
service_url = "https://todo-api-xxxxx-xx.a.run.app"
```

### GCP Deployment Architecture

When deployed to Google Cloud Platform using the provided Terraform configuration, the To-Do API uses the following architecture:

#### Cloud Services Used

1. **Cloud Run** - A fully managed serverless platform that automatically scales your containerized application based on traffic. The To-Do API runs as a stateless service on Cloud Run.

2. **Cloud SQL for PostgreSQL** - A fully managed relational database service that offers high availability, automated backups, and security features. The To-Do API connects to Cloud SQL using the Cloud SQL Socket Factory for secure connectivity.

3. **Google Container Registry (GCR)** - Stores and manages your Docker container images securely.

4. **IAM & Security** - The deployment uses Google Cloud's Identity and Access Management (IAM) to control access to resources. The Cloud Run service is configured with the necessary permissions to connect to Cloud SQL.

#### Connection Pattern

The application uses the Cloud SQL JDBC Socket Factory to establish a secure connection to the PostgreSQL database. This approach provides:
- Automatic IAM authentication
- Encrypted connections
- No need to expose the database to the public internet

#### Terraform Resources

The Terraform configuration creates and manages the following GCP resources:

1. **Cloud SQL Instance** - A PostgreSQL 13 database instance with automated backups
2. **Cloud SQL Database** - The "todo_db" database within the instance
3. **Cloud SQL User** - A database user with the necessary permissions
4. **Cloud Run Service** - The containerized To-Do API with appropriate environment variables
5. **IAM Permissions** - Necessary permissions for the services to communicate
6. **API Enablements** - Enables required GCP APIs for Cloud Run, Cloud SQL, and Artifact Registry

#### Scaling and Performance

The GCP deployment offers several advantages for scaling and performance:

1. **Automatic Scaling** - Cloud Run automatically scales based on incoming requests
2. **Zero Maintenance** - Both Cloud Run and Cloud SQL are fully managed services
3. **Global Availability** - Can be deployed to multiple regions for global availability
4. **Cost Efficiency** - Pay only for what you use with no minimum fees

## API Endpoints

- `GET /tasks` - List all tasks
- `GET /tasks/{id}` - Get a specific task
- `POST /tasks` - Create a new task
- `PUT /tasks/{id}` - Update a task
- `DELETE /tasks/{id}` - Delete a task
- `GET /health` - Health check endpoint

## Example Requests

### Create a task

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"title": "Learn Kotlin", "description": "Study Kotlin programming language"}'
```

### Get all tasks

```bash
curl http://localhost:8080/tasks
```

### Get a specific task

```bash
curl http://localhost:8080/tasks/1
```

### Update a task

```bash
curl -X PUT http://localhost:8080/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"title": "Learn Kotlin", "description": "Study Kotlin programming language", "completed": true}'
```

### Delete a task

```bash
curl -X DELETE http://localhost:8080/tasks/1
```

## Environment Variables

The application can be configured using the following environment variables:

### Local/Docker Environment
- `DB_HOST` - PostgreSQL host (default: localhost)
- `DB_PORT` - PostgreSQL port (default: 5432)
- `DB_NAME` - PostgreSQL database name (default: todo_db)
- `DB_USER` - PostgreSQL username (default: postgres)
- `DB_PASSWORD` - PostgreSQL password (default: postgres)
- `PORT` - Application port (default: 8080)

### GCP Environment
- `JDBC_URL` - JDBC URL for Cloud SQL connection
- `DB_USER` - Database username
- `DB_PASSWORD` - Database password

The application automatically detects whether it's running in a local environment or on GCP and uses the appropriate connection method.
