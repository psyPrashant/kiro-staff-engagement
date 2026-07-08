# Build-tool integration: Nx, Make, or plain Maven

All three funnel to the same underlying Maven command. Wire whichever the repo uses (Phase 0). The
tag filter must always pass through so a developer can scope a run to one story/slice during ATDD.

## The underlying Maven command

```bash
mvn clean test -B -Dcucumber.filter.tags="@story-<EPIC>-<N>"
```

**Run it from inside the module directory** (`cd {{MODULE_PATH}} && mvn …`), not with `-pl` from the
repo root. The acceptance module has no dependencies on other modules in the repo — it talks to the
running app over HTTP and the database over JDBC — so it builds standalone and does **not** need a
parent aggregator/reactor pom. Using `cd` makes it work in any repo layout: a single-app repo with no
root pom, a Gradle/Node backend, or a multi-module Maven build alike. (If the repo *is* a multi-module
Maven reactor and you registered the module in the parent `<modules>`, `-pl {{MODULE_PATH}}` from the
root also works — but `cd` always does.)

Environment variables the harness reads (set by the run target):

| Variable | Purpose | Example |
| --- | --- | --- |
| `APP_BASE_URL` | UI base URL the browser opens | `http://localhost:4200` |
| `ACCEPTANCE_DB_URL` | JDBC URL for the test DB | `jdbc:postgresql://localhost:5432/app_db` |
| `ACCEPTANCE_DB_USERNAME` / `ACCEPTANCE_DB_PASSWORD` | DB credentials | |
| `PLAYWRIGHT_HEADLESS` | `true` in CI, `false` to watch locally | `true` |

## Plain Maven (always works)

Run from inside the module directory:

```bash
cd {{MODULE_PATH}}

# one story
mvn clean test -Dcucumber.filter.tags="@story-<EPIC>-<N>"

# one slice
mvn clean test -Dcucumber.filter.tags="@slice-<EPIC>-<N>.<S>"

# everything except work-in-progress
mvn clean test -Dcucumber.filter.tags="not @wip"
```

Set the env vars inline or via a `.env`/profile. Document these commands in the module README even
when you also wire Nx or Make — they are the ground truth the others delegate to.

## Nx

Add `{{MODULE_PATH}}/project.json`. The `test` target forwards the `--tag` arg into the Maven tag
filter; env `configurations` capture headed vs headless and the local URLs.

```json
{
  "name": "acceptance-tests",
  "$schema": "../../node_modules/nx/schemas/project-schema.json",
  "projectType": "application",
  "sourceRoot": "{{MODULE_PATH}}/src",
  "targets": {
    "build": {
      "executor": "nx:run-commands",
      "options": { "command": "mvn clean package -DskipTests", "cwd": "{{MODULE_PATH}}" }
    },
    "test": {
      "executor": "nx:run-commands",
      "options": {
        "cwd": "{{MODULE_PATH}}",
        "command": "mvn clean test -B -Dcucumber.filter.tags=\"{args.tag}\""
      }
    },
    "clean": {
      "executor": "nx:run-commands",
      "options": { "command": "mvn clean", "cwd": "{{MODULE_PATH}}" }
    },
    "e2e": {
      "executor": "nx:run-commands",
      "defaultConfiguration": "headless",
      "options": {
        "command": "nx test acceptance-tests --tag=\"{args.tag}\"",
        "forwardAllArgs": false
      },
      "configurations": {
        "headed": {
          "env": {
            "PLAYWRIGHT_HEADLESS": "false",
            "APP_BASE_URL": "{{APP_BASE_URL}}",
            "ACCEPTANCE_DB_URL": "{{JDBC_URL}}",
            "ACCEPTANCE_DB_USERNAME": "{{DB_USER}}",
            "ACCEPTANCE_DB_PASSWORD": "{{DB_PASSWORD}}"
          }
        },
        "headless": {
          "env": {
            "PLAYWRIGHT_HEADLESS": "true",
            "APP_BASE_URL": "{{APP_BASE_URL}}",
            "ACCEPTANCE_DB_URL": "{{JDBC_URL}}",
            "ACCEPTANCE_DB_USERNAME": "{{DB_USER}}",
            "ACCEPTANCE_DB_PASSWORD": "{{DB_PASSWORD}}"
          }
        }
      }
    }
  }
}
```

Run it:

```bash
nx test acceptance-tests --tag="@story-<EPIC>-<N>"        # headless, via the test target
nx e2e  acceptance-tests --configuration=headed --tag="@slice-<EPIC>-<N>.<S>"
```

If the app must be booted first, add a `dependsOn` on the app's serve target. To run against a
dedicated throwaway stack instead of your dev environment, see
[isolated-testing](isolated-testing.md).

## Make

**This is the primary entry point when the project has no Nx** — which is the common case. The
Makefile is a complete, self-sufficient interface to the harness; you do not need Nx for anything
below. Add these targets to the root `Makefile` (or an included `acceptance.mk`). `TAG` defaults to a
not-`@wip` filter so a bare `make accept-test` runs the whole suite.

```makefile
MODULE      := {{MODULE_PATH}}
TAG         ?= not @wip
APP_URL     ?= {{APP_BASE_URL}}

export APP_BASE_URL            ?= {{APP_BASE_URL}}
export ACCEPTANCE_DB_URL       ?= {{JDBC_URL}}
export ACCEPTANCE_DB_USERNAME  ?= {{DB_USER}}
export ACCEPTANCE_DB_PASSWORD  ?= {{DB_PASSWORD}}

.PHONY: accept-test accept-test-headed accept-smoke accept-compile \
        accept-build accept-clean accept-install

accept-test:                      ## Run acceptance tests headless. Scope with TAG='@story-...'
	cd $(MODULE) && PLAYWRIGHT_HEADLESS=true \
	mvn clean test -B -Dcucumber.filter.tags="$(TAG)"

accept-test-headed:               ## Run acceptance tests in a visible browser
	cd $(MODULE) && PLAYWRIGHT_HEADLESS=false \
	mvn clean test -B -Dcucumber.filter.tags="$(TAG)"

accept-smoke:                     ## Fast tagged subset for pre-push / quick checks
	cd $(MODULE) && PLAYWRIGHT_HEADLESS=true \
	mvn -q test -Dcucumber.filter.tags="@pre-push"

accept-compile:                   ## Compile tests only — no browser, seconds. Great for pre-push.
	cd $(MODULE) && mvn -q test-compile

accept-build:                     ## Package the module without running tests
	cd $(MODULE) && mvn clean package -DskipTests

accept-clean:
	cd $(MODULE) && mvn clean

accept-install:                   ## Download Playwright browser binaries (run once, if needed)
	cd $(MODULE) && mvn -q exec:java \
	  -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"
```

> Each recipe runs in its own subshell, so the `cd` does not leak between targets. `$(MODULE)` is
> relative to the repo root where `make` is invoked.

Run it:

```bash
make accept-install                                   # once, to fetch browsers
make accept-compile                                   # fast wiring check
make accept-test TAG="@story-<EPIC>-<N>"
make accept-test-headed TAG="@slice-<EPIC>-<N>.<S>"
make accept-smoke                                     # the @pre-push subset
```

Every target just wraps a `mvn` command, so a developer with no Make can run the equivalent
plain-Maven command from the section above — the Makefile is convenience, not a dependency.

> On Windows without GNU Make, use the plain-Maven commands directly (or run Make under WSL / Git
> Bash). The recipes assume a POSIX shell.

## Wire whichever the project uses

- **No Nx (the common case):** wire Make (above) and/or document the plain-Maven commands. That is a
  complete setup — nothing here needs Nx.
- **Nx present:** add the `project.json` target too.
- **Both present:** add both; they call the same Maven command, so they stay consistent. Point the
  module README at whichever the team treats as canonical.
