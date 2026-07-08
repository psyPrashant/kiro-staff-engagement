# Implementation Plan: Containerization

## Overview

Containerize the Staff Engagement monorepo by creating multi-stage Dockerfiles for backend and frontend, an nginx configuration for SPA routing and API proxying, Docker Compose orchestration for the full local stack, and .dockerignore files for build efficiency. All configuration files are declarative (Dockerfile, YAML, nginx conf).

## Tasks

- [x] 1. Create backend Docker build artifacts
  - [x] 1.1 Create backend .dockerignore file
    - Create `staff-engagement-backend/.dockerignore` excluding `target/`, `.git/`, `.idea/`, `.vscode/`, `*.iml`, `*.log`, `.gitignore`, `HELP.md`
    - _Requirements: 4.3_

  - [x] 1.2 Create backend multi-stage Dockerfile
    - Create `staff-engagement-backend/Dockerfile` with:
    - Build stage (`builder`): base `eclipse-temurin:21-jdk-alpine`, copy `pom.xml`, `.mvn/`, `mvnw` first, `chmod +x mvnw`, run `./mvnw dependency:go-offline -B`, then copy `src/` and run `./mvnw package -DskipTests -B`
    - Runtime stage: base `eclipse-temurin:21-jre-alpine`, create non-root user `appuser` (UID 1000), copy JAR from builder, expose port 8080, ENTRYPOINT `java -jar /app/app.jar`, run as `appuser`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 4.1, 4.5_

- [x] 2. Create frontend Docker build artifacts
  - [x] 2.1 Create frontend .dockerignore file
    - Create `staff-engagement-frontend/.dockerignore` excluding `node_modules/`, `dist/`, `.git/`, `.idea/`, `.vscode/`, `.angular/`, `*.log`, `.gitignore`
    - _Requirements: 4.4_

  - [x] 2.2 Create nginx configuration for SPA and API proxy
    - Create `staff-engagement-frontend/nginx/default.conf` with:
    - `listen 8080;` (nginx-unprivileged cannot bind port 80)
    - SPA fallback: `try_files $uri $uri/ /index.html;`
    - API proxy: `location /api/` proxying to `http://backend:8080/api/` with forwarded headers
    - _Requirements: 2.4, 3.6_

  - [x] 2.3 Create frontend multi-stage Dockerfile
    - Create `staff-engagement-frontend/Dockerfile` with:
    - Build stage (`builder`): base `node:20-alpine`, copy `package.json` and `package-lock.json` first, run `npm ci`, copy remaining source, run `npx ng build --configuration production`
    - Runtime stage: base `nginxinc/nginx-unprivileged:alpine`, copy compiled assets from `builder` stage `dist/staff-engagement/browser/` into `/usr/share/nginx/html/`, copy custom nginx config into `/etc/nginx/conf.d/`, expose port 8080
    - _Requirements: 2.1, 2.2, 2.3, 2.5, 2.6, 2.7, 4.2, 4.6_

- [x] 3. Checkpoint - Verify individual image builds
  - Ensure `docker build` completes for both backend and frontend Dockerfiles individually, ask the user if questions arise.

- [x] 4. Create Docker Compose orchestration
  - [x] 4.1 Create docker-compose.yml at repository root
    - Create `docker-compose.yml` defining three services:
    - `database`: `postgres:16-alpine`, env vars `POSTGRES_DB=staff_engagement`, `POSTGRES_USER=postgres`, `POSTGRES_PASSWORD=admin`, named volume `pgdata`, health check with `pg_isready`
    - `backend`: build context `./staff-engagement-backend`, port `8080:8080`, env vars `SPRING_PROFILES_ACTIVE=dev`, `DB_HOST=database`, `DB_PORT=5432`, `DB_NAME=staff_engagement`, `DB_USERNAME=postgres`, `DB_PASSWORD=admin`, `depends_on` database with `condition: service_healthy`, health check via wget to `/actuator/health` with `start_period: 30s`
    - `frontend`: build context `./staff-engagement-frontend`, port `4200:8080`, `depends_on` backend with `condition: service_healthy`
    - Named volume `pgdata` at top level
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10_

- [x] 5. Final checkpoint - Full stack verification
  - Ensure `docker compose up` starts all services successfully, frontend is accessible at `http://localhost:4200`, backend health endpoint responds at `http://localhost:8080/actuator/health`, and API proxy works at `http://localhost:4200/api/actuator/health`. Ask the user if questions arise.

## Notes

- No test sub-tasks are included because this feature produces declarative infrastructure configuration files, not application code with testable functions. The design explicitly states property-based testing does not apply.
- Verification is performed via `docker compose build` and `docker compose up` at checkpoint tasks.
- Each task references specific requirement acceptance criteria for traceability.
- The backend's existing `application-dev.properties` already reads `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` from environment variables — no application code changes are needed.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1"] },
    { "id": 1, "tasks": ["1.2", "2.2"] },
    { "id": 2, "tasks": ["2.3"] },
    { "id": 3, "tasks": ["4.1"] }
  ]
}
```
