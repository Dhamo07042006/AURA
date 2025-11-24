# Step 1: Use official Java runtime as base image
FROM eclipse-temurin:17-jdk-alpine

# Step 2: Set working directory
WORKDIR /app

# Step 3: Copy the built JAR into the container
COPY target/*.jar app.jar

# Step 4: Expose the port (Render will provide PORT env)
EXPOSE 8080

# Step 5: Run the JAR
ENTRYPOINT ["java","-jar","app.jar"]
