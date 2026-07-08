# Tech Stack

## Backend (`staff-engagement-backend/`)

| Concern | Technology |
|---------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Build tool | Maven (wrapper included: `mvnw` / `mvnw.cmd`) |
| Database | PostgreSQL (runtime dependency) |
| ORM | Spring Data JPA |
| Monitoring | Spring Boot Actuator |
| Code gen | Lombok (annotation processing configured in maven-compiler-plugin) |
| Testing | Spring Boot Test (JUnit 5) |

### Backend Commands

```bash
# Build (from staff-engagement-backend/)
./mvnw clean package

# Run
./mvnw spring-boot:run

# Test
./mvnw test
```

## Frontend (`staff-engagement-frontend/`)

| Concern | Technology |
|---------|-----------|
| Language | TypeScript 5.9 (strict mode) |
| Framework | Angular 21 (standalone components, signals) |
| Build tool | Angular CLI via `@angular/build` |
| Package manager | npm 11 |
| Test runner | Vitest 4 with jsdom |
| Formatting | Prettier (single quotes, 100 char line width, Angular HTML parser) |
| Styling | Plain CSS (component-scoped + global `styles.css`) |

### Frontend Commands

```bash
# Install dependencies (from staff-engagement-frontend/)
npm install

# Dev server
npm start        # ng serve (default port 4200)

# Production build
npm run build    # ng build --configuration production

# Run tests (single execution)
npx vitest --run

# Format code
npx prettier --write .
```

## Code Style Conventions

- **Frontend indentation:** 2 spaces (enforced by `.editorconfig`)
- **Backend indentation:** tabs (Spring Boot default)
- **TypeScript quotes:** single quotes
- **Print width:** 100 characters
- **Final newline:** always
- **TypeScript strict flags:** `strict`, `noImplicitOverride`, `noImplicitReturns`, `noFallthroughCasesInSwitch`
- **Angular strict templates:** enabled
