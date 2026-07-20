# Project Structure

Monorepo with two independent sub-projects at the root level:

```
kiro-staff-engagement/
├── .kiro/                          # Kiro IDE configuration & steering
├── staff-engagement-backend/       # Spring Boot API (modular monolith)
│   ├── src/main/java/com/psybergate/staff_engagement/
│   │   ├── StaffEngagementApplication.java   # Entry point
│   │   ├── common/                           # Cross-cutting: ClockConfig, error handling
│   │   └── <module>/                         # One package per business capability
│   │       ├── web/                          # @RestController
│   │       ├── service/                      # Service interface + <Name>ServiceImpl
│   │       ├── domain/                       # @Entity, repositories, enums, exceptions
│   │       ├── dto/                          # Request/response records
│   │       └── config/                       # Module-scoped Spring config (where needed)
│   ├── src/main/resources/
│   │   ├── application.properties            # App configuration
│   │   └── db/migration/                     # Flyway migrations
│   ├── src/test/java/com/psybergate/staff_engagement/
│   ├── pom.xml                               # Maven build config
│   ├── mvnw / mvnw.cmd                       # Maven wrapper scripts
│   └── .mvn/                                 # Maven wrapper JARs
└── staff-engagement-frontend/      # Angular SPA
    ├── src/
    │   ├── app/
    │   │   ├── app.ts              # Root component (standalone)
    │   │   ├── app.html            # Root template
    │   │   ├── app.css             # Root styles
    │   │   ├── app.config.ts       # Application providers
    │   │   └── app.routes.ts       # Route definitions
    │   ├── main.ts                 # Bootstrap entry
    │   ├── index.html              # HTML shell
    │   └── styles.css              # Global styles
    ├── public/                     # Static assets
    ├── angular.json                # Angular workspace config
    ├── tsconfig.json               # TypeScript project references root
    ├── tsconfig.app.json           # App-specific TS config
    ├── tsconfig.spec.json          # Test-specific TS config
    └── package.json                # npm dependencies & scripts
```

## Backend Modules

Each module is a business capability, not a technical layer. Current modules:

| Module | Responsibility |
|--------|----------------|
| `auth` | Session login/logout, security configuration |
| `user` | Application users (the people doing the engaging) |
| `employee` | Employees being engaged with |
| `client` | Companies and the projects belonging to them |
| `interaction` | Logged interactions between a user and an employee |
| `task` | Follow-up tasks, optionally linked to an interaction |
| `scheduling` | Future scheduled interactions and overdue tracking |
| `engagement` | Engagement matrix — recency/frequency scoring |
| `employee360` | Read-only aggregate view across the above |
| `seed` | Demo data loading (dev profiles only) |
| `common` | Cross-cutting configuration and error handling |

## Conventions

- **Backend package root:** `com.psybergate.staff_engagement`
- **Backend services:** every service is an interface (`TaskService`) with exactly one
  implementation (`TaskServiceImpl`) in the same `service` package. Controllers, other
  services and tests depend on the **interface** — nothing outside a `service` package
  should name an `Impl` type.
- **Backend layering:** `web → service → domain`. Controllers must not inject
  repositories directly; go through the module's service.
- **Backend DTOs:** request/response records live in the module's `dto` package, never
  at the module root.
- **Backend entities:** JPA entities and their Spring Data repositories live together in
  the module's `domain` package, alongside module-specific exceptions and enums.
- **Angular component prefix:** `app`
- **Angular components:** standalone (no NgModules)
- **Angular state management:** signals (not zone-based change detection)
- **Routing:** centralized in `app.routes.ts`
- **Providers:** centralized in `app.config.ts`

## Adding New Features

- **Backend:** Add a new capability as a new module package under
  `com.psybergate.staff_engagement`, with the `web` / `service` / `domain` / `dto`
  layout above. Reuse an existing module rather than creating a thin one that only
  wraps another module's entities.
- **Frontend:** Create feature directories under `src/app/` with standalone components. Register routes in `app.routes.ts`. Add global providers in `app.config.ts`.
