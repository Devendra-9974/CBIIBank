# Multi-stage Dockerfile for Spring Boot Online Banking Application
# Stage 1: Build stage with Maven & JDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app

# Cache dependencies
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production jar
COPY backend/src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal JRE Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root system user for banking security compliance
RUN addgroup -S bankgroup && adduser -S bankuser -G bankgroup
USER bankuser

# Copy jar from builder
COPY --from=builder --chown=bankuser:bankgroup /app/target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Environment defaults
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    DB_URL=jdbc:mysql://mysql-db:3306/bank_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC \
    DB_USERNAME=root \
    DB_PASSWORD=root \
    JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970 \
    JWT_EXPIRATION_MS=86400000

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/swagger-ui.html || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
