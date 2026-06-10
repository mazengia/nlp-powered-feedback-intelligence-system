# Multi-stage build for Spring Boot application
# Stage 1: Build
FROM maven:3.9.3-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy only pom.xml first (for better layer caching)
COPY pom.xml .

# Download dependencies
RUN mvn dependency:resolve-plugins dependency:resolve

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# Create non-root user for security
RUN useradd -m -u 1000 appuser

# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Create directories for logs and config
RUN mkdir -p /var/log/feedback-analyzer && \
    chown -R appuser:appuser /app /var/log/feedback-analyzer

# Switch to non-root user
USER appuser

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -Xmx512m" \
    SERVER_PORT=8080

