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
├── build.gradle.kts
├── gradle.properties
├── Dockerfile
└── docker-compose.yml
```

## Prerequisites

- JDK 17+
- Gradle
- Docker and Docker Compose (for containerized deployment)

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

- `DB_HOST` - PostgreSQL host (default: localhost)
- `DB_PORT` - PostgreSQL port (default: 5432)
- `DB_NAME` - PostgreSQL database name (default: todo_db)
- `DB_USER` - PostgreSQL username (default: todo_user)
- `DB_PASSWORD` - PostgreSQL password (default: todo_password)
- `PORT` - Application port (default: 8080)
