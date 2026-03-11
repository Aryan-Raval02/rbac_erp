# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy the compiled jar from the build stage
COPY --from=build /app/target/rbac.jar app.jar

# Add a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Expose the port the app runs on (default Spring Boot port)
EXPOSE 8080

# Environment variables for JVM tuning (can be overridden by docker run)
# We set max heap to 75% of container RAM to leave room for off-heap memory
ENV JAVA_OPTS="-XX:InitialRAMPercentage=50.0 -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

# Run the application using shell form to interpolate JAVA_OPTS
ENTRYPOINT sh -c 'java $JAVA_OPTS -jar app.jar'
