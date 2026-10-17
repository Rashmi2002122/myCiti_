# Multi-stage Dockerfile for Spring Boot
# Stage 1: Build the application using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Use a custom settings.xml that points to repo1.maven.org to avoid
# 429 rate-limits on repo.maven.apache.org from Render's shared build IPs.
COPY settings.xml /root/.m2/settings.xml
COPY pom.xml .
COPY src ./src
# Retry loop: Maven Central rate-limits (429) Render's shared IPs.
# Wait 15 s between attempts — up to 5 tries.
RUN for i in $(seq 1 5); do \
      echo "=== Maven build attempt $i/5 ==="; \
      mvn clean package -DskipTests -B && break; \
      EXIT=$?; \
      if [ $i -lt 5 ]; then echo "Attempt $i failed (exit $EXIT), retrying in 15s..."; sleep 15; fi; \
    done; \
    test -f target/myciti-backend-0.0.1-SNAPSHOT.jar || (echo "Build failed after 5 attempts" && exit 1)

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
