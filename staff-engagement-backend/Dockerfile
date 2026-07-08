# ---- Build stage ----
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy dependency-related files first (changes rarely → cached layer)
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw ./

# Ensure Maven wrapper is executable and download dependencies
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code (changes frequently) and build
COPY src/ ./src/
RUN ./mvnw package -DskipTests -B

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre-alpine

# Create non-root user
RUN adduser -D -u 1000 appuser

WORKDIR /app

# Copy only the executable JAR from the build stage
COPY --from=builder /app/target/*.jar /app/app.jar

EXPOSE 8080

USER appuser

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
