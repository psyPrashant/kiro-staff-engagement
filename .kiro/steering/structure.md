# Project Structure

Monorepo with two independent sub-projects at the root level:

```
kiro-staff-engagement/
├── .kiro/                          # Kiro IDE configuration & steering
├── staff-engagement-backend/       # Spring Boot API
│   ├── src/main/java/com/psybergate/staff_engagement/
│   │   └── StaffEngagementApplication.java   # Entry point
│   ├── src/main/resources/
│   │   └── application.properties            # App configuration
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

## Conventions

- **Backend package root:** `com.psybergate.staff_engagement`
- **Angular component prefix:** `app`
- **Angular components:** standalone (no NgModules)
- **Angular state management:** signals (not zone-based change detection)
- **Routing:** centralized in `app.routes.ts`
- **Providers:** centralized in `app.config.ts`

## Adding New Features

- **Backend:** Create packages under `com.psybergate.staff_engagement` following standard Spring layering (controller, service, repository, model/entity).
- **Frontend:** Create feature directories under `src/app/` with standalone components. Register routes in `app.routes.ts`. Add global providers in `app.config.ts`.
