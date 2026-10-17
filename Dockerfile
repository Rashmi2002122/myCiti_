# Multi-stage Dockerfile for Spring Boot
# Stage 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build executable jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Add non-root user for security
RUN groupadd -r myciti && useradd -r -g myciti myciti

# Copy jar from builder stage
COPY --from=builder /app/target/myciti-backend-0.0.1-SNAPSHOT.jar app.jar

# Set ownership
RUN chown -R myciti:myciti /app
USER myciti

# Render dynamically injects $PORT (default to 8080)
EXPOSE 8080

# Configure JVM options suited for cloud containers with low memory
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
