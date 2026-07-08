# Isolated testing (local)

By default the harness runs against whatever stack you already have up — your dev app on
`localhost:4200` and your dev database. That's fine for writing a test, but it has two problems: the
tests mutate your dev database, and a run depends on you having manually started the stack.

**Isolated testing** solves both: bring up a dedicated, throwaway stack on its **own ports** with its
**own fresh database**, run the tests against it headless, then tear it all down. Nothing touches your
dev environment, and the run is self-contained — one command up, one command down.

This is a local convenience, not a CI concern. The point is a clean, repeatable run on your machine.

## The shape

```
start db + backend + ui (+ email capture) on isolated ports
        ↓
wait until every health endpoint responds
        ↓
run the acceptance tests headless, pointed at the isolated ports
        ↓
tear the whole stack down — even if the tests failed
```

Always sequential, and always tear down (use a shell trap so a failed run still cleans up).

## Isolation levers

- **Distinct ports** — offset from your dev defaults so an isolated run never clashes with a dev stack
  you left running. E.g. dev UI `4200` → isolated `14200`, dev API `8080` → `18080`, dev DB `5432` →
  `15432`.
- **A throwaway database** — a fresh container (or a dedicated `_iso` schema) that starts empty and is
  discarded on teardown. The `global-cleanup.sql` watermark still applies within a run; the fresh DB
  just guarantees you never start from someone else's leftovers.
- **Headless** — `PLAYWRIGHT_HEADLESS=true`; there's no one watching an isolated run.

## Portable recipe — Docker Compose (works with or without Nx)

A compose file is the most portable way to describe the isolated stack. Keep it beside the module,
e.g. `{{MODULE_PATH}}/isolated/docker-compose.yml`, bringing up the DB, backend, UI, and (if the app
sends email) a mail-capture container — each on an isolated port.

```yaml
# {{MODULE_PATH}}/isolated/docker-compose.yml  (illustrative — match your app's images/build)
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: app_db
      POSTGRES_USER: {{DB_USER}}
      POSTGRES_PASSWORD: {{DB_PASSWORD}}
    ports: ["15432:5432"]
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U {{DB_USER}}"]
      interval: 3s
      retries: 20

  backend:
    build: ../../..            # or image: your-backend:local
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/app_db
    ports: ["18080:8080"]
    depends_on:
      db: { condition: service_healthy }

  ui:
    build: ../../../<ui-module>   # or image: your-ui:local
    ports: ["14200:80"]
    depends_on: [backend]
```

Then a Make target orchestrates up → wait → test → down:

```makefile
ISO_DIR   := $(MODULE)/isolated
ISO_APP   := http://localhost:14200
ISO_API   := http://localhost:18080/actuator/health
ISO_DB     := jdbc:postgresql://localhost:15432/app_db

.PHONY: accept-isolated
accept-isolated:                  ## Bring up a throwaway stack, run tests headless, tear down
	docker compose -f $(ISO_DIR)/docker-compose.yml up -d --build
	trap 'docker compose -f $(ISO_DIR)/docker-compose.yml down -v' EXIT; \
	npx wait-on --timeout 120000 $(ISO_API) $(ISO_APP) || command -v wait-on >/dev/null || \
	  { echo "install wait-on or poll with curl"; exit 1; }; \
	cd $(MODULE) && \
	  APP_BASE_URL=$(ISO_APP) \
	  ACCEPTANCE_DB_URL=$(ISO_DB) \
	  ACCEPTANCE_DB_USERNAME=$(DB_USER) \
	  ACCEPTANCE_DB_PASSWORD=$(DB_PASSWORD) \
	  PLAYWRIGHT_HEADLESS=true \
	  mvn clean test -Dcucumber.filter.tags="$(TAG)"
```

The `trap ... EXIT` guarantees teardown even when the tests fail. If you don't want a `wait-on`
dependency, poll the health URLs with `curl --retry` in a small loop instead — the only requirement is
"don't start the tests until the stack answers."

Run it:

```bash
make accept-isolated TAG="@story-<EPIC>-<N>"
```

## Nx variant (only if the repo uses Nx)

If the app already exposes isolated serve targets, an Nx `isolated` target can chain them — depend on
the serve targets, `wait-on` the health endpoints, run the test target, then a down target — all with
`"parallel": false` so the steps run in order:

```json
"isolated": {
  "executor": "nx:run-commands",
  "dependsOn": ["build", { "projects": "<app>", "target": "serve-isolated" }],
  "options": {
    "parallel": false,
    "forwardAllArgs": false,
    "commands": [
      "npx wait-on http://localhost:18080/actuator/health",
      "npx wait-on http://localhost:14200",
      "nx test acceptance-tests --tag=\"{args.tag}\"",
      "nx run <app>:isolated-down"
    ],
    "env": {
      "APP_BASE_URL": "http://localhost:14200",
      "ACCEPTANCE_DB_URL": "jdbc:postgresql://localhost:15432/app_db",
      "PLAYWRIGHT_HEADLESS": "true"
    }
  }
}
```

Most non-Nx projects won't have `serve-isolated`/`isolated-down` targets — the Docker Compose recipe
above is the portable equivalent and is the recommended default.

## Rules

- **Sequential, never parallel** — start, wait, test, stop in strict order.
- **Always tear down** — wrap teardown in a `trap`/`finally` so a failed test still stops the stack and
  frees the ports.
- **Fresh DB per run** — `docker compose down -v` drops the volume so the next run starts clean.
- **Never reuse dev ports** — that's the whole point; a dev stack and an isolated run must be able to
  coexist.
- **Point the harness via env vars only** — the same `APP_BASE_URL` / `ACCEPTANCE_DB_*` the normal run
  uses, just set to the isolated values. No code or property-file changes.
