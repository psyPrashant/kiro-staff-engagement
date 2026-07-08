# Detect the stack and resolve placeholders

Every template in this skill uses placeholders. Resolve them all before writing files. A wrong base
package, DB URL, or module path breaks the whole harness, so confirm anything ambiguous with the user.

## 1. Build tool

Check the repo root, in this order:

| Signal | Build tool | Wire |
| --- | --- | --- |
| `nx.json` and/or per-project `project.json` | **Nx** | an Nx `test` target (see build-tool-integration) |
| a root `Makefile` | **Make** | `accept-*` Make targets |
| neither | **plain Maven** | document the raw `mvn` command |

A repo can have **both** Nx and Make. Wire whichever the team runs day to day; if unclear, ask, or
wire both (they both just call Maven). Do not assume Nx — the target project may have none.

## 2. Module location

Match the repo's existing convention:

- **Nx monorepo** — apps live under `apps/`, so `apps/acceptance-tests`.
- **Multi-module Maven** — add a submodule and register it in the parent `pom.xml` `<modules>`.
- **Single Maven app** — a top-level `acceptance-tests/` directory kept as its own module is simplest.

`{{MODULE_PATH}}` is this directory relative to the repo root.

## 3. Base Java package

Mirror the backend. If the app is `com.example.app`, use `com.example.acceptance` for the tests.
`{{BASE_PACKAGE_PATH}}` is that package with dots as slashes (`com/example/acceptance`).

The Spring `@Configuration` sits just **outside** the scanned package to avoid a self-scan cycle —
the templates use `{{BASE_PACKAGE}}.acceptanceinfra.config` for it and component-scan
`{{BASE_PACKAGE}}.acceptance`. `{{INFRA_PACKAGE_PATH}}` is `.../acceptanceinfra` as a path.

## 4. Database

Find the local/test DB the app uses (look in the backend's `application.yml`/`.properties`, docker
compose, or `.env`). Resolve:

- `{{JDBC_URL}}` — e.g. `jdbc:postgresql://localhost:5432/app_db`
- `{{DB_USER}}` / `{{DB_PASSWORD}}`
- `{{JDBC_DRIVER}}` — driver class, e.g. `org.postgresql.Driver`
- `{{JDBC_DEP}}` — the Maven dependency for that driver

The worked examples use PostgreSQL. For MySQL/others, swap the driver class, the Maven dependency,
and adjust the SQL dialect in `global-cleanup.sql` / `global-sequence-init.sql` (sequence-reset
syntax differs across databases — see lifecycle-and-seeding).

## 5. Application URLs

- `{{APP_BASE_URL}}` — where the UI is served locally (e.g. `http://localhost:4200`).
- Optional `{{TEST_SUPPORT_BASE_URL}}` — a backend test-support/REST base the API driver hits to
  verify state independently of the UI. Only needed if you add an API driver.
- Optional `{{EMAIL_BASE_URL}}` — a test email inbox API (e.g. a local SMTP-capture tool) if the app
  sends email and you plan to assert on it.

## Substitution table (fill this in, then apply everywhere)

```
{{BASE_PACKAGE}}          = com.example.acceptance
{{BASE_PACKAGE_PATH}}     = com/example/acceptance
{{INFRA_PACKAGE_PATH}}    = com/example/acceptanceinfra
{{GROUP_ID}}              = com.example
{{ARTIFACT_ID}}           = acceptance-tests
{{MODULE_PATH}}           = apps/acceptance-tests
{{APP_BASE_URL}}          = http://localhost:4200
{{JDBC_URL}}              = jdbc:postgresql://localhost:5432/app_db
{{DB_USER}}               = app-admin
{{DB_PASSWORD}}           = local-dev
{{JDBC_DRIVER}}           = org.postgresql.Driver
```

Keep the filled-in table in front of you while creating files.

## Two kinds of placeholder — don't confuse them

- `{{DOUBLE_BRACE}}` — **build-time**, substitute now while scaffolding (package names, module path,
  Maven coordinates, and the *default* URLs baked into a Makefile/`project.json`).
- `${DOLLAR_BRACE}` — **runtime** Spring/property references (as in `assets/application.properties`:
  `app_base_url=${APP_BASE_URL}`). **Leave these exactly as they are.** They are resolved from
  environment variables when the tests run, which is what lets one build run against local, CI, or an
  ephemeral environment without editing files. The only file that keeps `${...}` verbatim is
  `application.properties`.
