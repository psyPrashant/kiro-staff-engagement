# Requirements Document

## Introduction

Containerize the Staff Engagement backend and frontend applications and provide a single-command local development environment using Docker Compose. This enables the team to spin up the full stack (frontend, backend, database) consistently without manual setup of individual services.

## Glossary

- **Backend_Image**: The Docker container image for the Spring Boot backend service, built via a multi-stage Dockerfile.
- **Frontend_Image**: The Docker container image for the Angular frontend, built via a multi-stage Dockerfile and served by nginx.
- **Compose_Stack**: The Docker Compose orchestration defining all services (backend, frontend, database) for the local environment.
- **Nginx_Server**: The nginx reverse proxy/web server embedded in the Frontend_Image that serves the Angular production bundle.
- **Database_Service**: The PostgreSQL container provisioned by Docker Compose with a persistent volume.
- **Maven_Wrapper**: The vendored Maven build tool (`mvnw`) used to compile and package the backend application.
- **Angular_CLI**: The Angular build tooling invoked via `ng build --configuration production` to produce the frontend production bundle.
- **Layer_Caching**: Docker's mechanism for reusing unchanged build layers to speed up subsequent image builds.

## Requirements

### Requirement 1: Backend Dockerfile

**User Story:** As a developer, I want a multi-stage Docker build for the backend, so that the resulting image is small and builds quickly using layer caching.

#### Acceptance Criteria

1. THE Backend_Image build SHALL use a multi-stage Dockerfile with a build stage and a runtime stage.
2. WHEN building the Backend_Image, THE build stage SHALL use the Maven_Wrapper to compile and package the application, skipping tests, and the Maven_Wrapper script SHALL have executable permissions in the build stage.
3. THE Backend_Image runtime stage SHALL use a JRE 21 base image that contains no build tools, compiler, or source utilities (e.g., eclipse-temurin:21-jre-alpine or equivalent JRE-only image).
4. WHEN only source code files have changed and `pom.xml` plus the `.mvn/` directory remain unchanged, THE Backend_Image build SHALL reuse the cached dependency layer without re-downloading dependencies.
5. THE Backend_Image SHALL copy only the final executable JAR into the runtime stage.
6. THE Backend_Image SHALL expose port 8080 for HTTP traffic.
7. THE Backend_Image SHALL run the application as a non-root user with a dedicated system account (UID >= 1000).
8. THE Backend_Image SHALL define an ENTRYPOINT or CMD that launches the executable JAR using `java -jar`, so that the container starts the Spring Boot application when run with default arguments.

### Requirement 2: Frontend Dockerfile

**User Story:** As a developer, I want a multi-stage Docker build for the frontend, so that the production bundle is served efficiently via nginx with proper Angular routing support.

#### Acceptance Criteria

1. THE Frontend_Image build SHALL use a multi-stage Dockerfile with a build stage and a runtime stage.
2. WHEN building the Frontend_Image, THE build stage SHALL use a Node.js 20 or later base image, install npm dependencies, and run `ng build --configuration production` to produce the production bundle.
3. THE Frontend_Image runtime stage SHALL use an nginx base image and copy the compiled static assets from the build stage's Angular output directory into the nginx default serving directory.
4. WHEN a request path does not match a static file, THE Nginx_Server SHALL return `index.html` with HTTP status 200 to support Angular client-side routing.
5. THE Frontend_Image SHALL expose port 80 for HTTP traffic.
6. WHEN dependencies listed in `package.json` and `package-lock.json` have not changed, THE Frontend_Image build SHALL reuse the cached npm install layer.
7. THE Frontend_Image SHALL run the nginx process as a non-root user.

### Requirement 3: Docker Compose Local Stack

**User Story:** As a developer, I want a single Docker Compose command to start the full local environment, so that I can develop and test without manually configuring each service.

#### Acceptance Criteria

1. THE Compose_Stack SHALL define three services: backend, frontend, and database.
2. THE Database_Service SHALL use a PostgreSQL image and store data in a named Docker volume for persistence across restarts.
3. THE Database_Service SHALL be configured with a database name, username, and password matching the backend connection settings.
4. WHEN the Compose_Stack starts, THE Backend_Image SHALL receive database connection configuration via environment variables or service-level configuration.
5. IF the Database_Service is not yet accepting connections, THEN THE Backend_Image SHALL retry connection attempts for up to 60 seconds before failing with a non-zero exit code.
6. THE Frontend_Image SHALL proxy requests with the path prefix `/api/` to the Backend_Image on port 8080 within the Compose network.
7. WHEN `docker compose up` is executed, THE Compose_Stack SHALL build and start all services such that the frontend serves the application page, the backend responds to health-check requests, and the database accepts connections.
8. THE Compose_Stack SHALL map the frontend to host port 4200 so that the application is accessible from the developer's browser.
9. THE Compose_Stack SHALL map the backend to host port 8080 so that API endpoints are directly accessible for debugging.
10. IF `docker compose down` is executed followed by `docker compose up`, THEN THE Database_Service SHALL retain data created during the previous session via the named volume.

### Requirement 4: Image Size and Build Efficiency

**User Story:** As a developer, I want the container images to be as small as possible, so that they build quickly and consume minimal storage.

#### Acceptance Criteria

1. THE Backend_Image final stage SHALL exclude build tools, source code, intermediate compilation output, and the Maven local repository.
2. THE Frontend_Image final stage SHALL contain only the nginx binary and its dependencies, the nginx configuration file, and the compiled static assets from the Angular production build.
3. THE backend project directory SHALL include a `.dockerignore` file that excludes at minimum: `target/`, `.git/`, `.idea/`, `.vscode/`, `*.iml`, and `*.log`.
4. THE frontend project directory SHALL include a `.dockerignore` file that excludes at minimum: `node_modules/`, `dist/`, `.git/`, `.idea/`, `.vscode/`, `.angular/`, and `*.log`.
5. WHEN the Backend_Image is built, THE final image size SHALL be no larger than 300 MB.
6. WHEN the Frontend_Image is built, THE final image size SHALL be no larger than 50 MB.
