# Multi-stage build for optimized Docker image
FROM maven:3.9.6-amazoncorretto-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml first to leverage Docker cache
COPY pom.xml .

# Download dependencies (cached if pom.xml hasn't changed)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage with smaller image
FROM amazoncorretto:17-alpine

# Set working directory
WORKDIR /app

# Copy the jar file from build stage
COPY --from=build /app/target/spring-backend-1.0.0.jar app.jar

# Create a non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Change ownership of the app directory
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port 5000 (matching your application.properties)
EXPOSE 5000

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:5000/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# JVM optimization for containerized environments
CMD ["-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-Dspring.profiles.active=docker"]