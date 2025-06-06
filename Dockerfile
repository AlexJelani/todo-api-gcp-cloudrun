FROM gradle:7.6-jdk17 as build

WORKDIR /app
COPY . .
# Use the application plugin's installDist task
RUN gradle installDist --no-daemon

FROM openjdk:17-slim

WORKDIR /app
# Copy the entire distribution
COPY --from=build /app/build/install/todo-api ./

EXPOSE 8080

# Run the start script
CMD ["./bin/todo-api"]
