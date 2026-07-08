---
name: cucumber-acceptance-setup
description: >
  Scaffold a four-layer Cucumber + Playwright + Spring acceptance-test module into a Java + Angular
  (or any Java backend + web UI) project from scratch. Use this skill when a project has no
  acceptance-test harness yet and you need to stand one up: create the Maven test module, wire the
  Spring context that owns the Playwright browser, add the JUnit Platform Suite runner, the SQL
  script runner, global data-cleanup hooks, scenario-scoped TestWorld state, and the build-tool
  targets. Caters for Nx, Make, or plain Maven — it detects which the repo uses and wires the
  matching run target. Triggers on: "set up acceptance tests", "scaffold cucumber", "add an
  acceptance-test module", "bootstrap BDD tests", "create the acceptance-test harness", "no
  acceptance tests yet", or any request to introduce Cucumber/Playwright acceptance testing to a
  Java project. For writing individual tests once the harness exists, use cucumber-acceptance-authoring instead.
compatibility: >
  Targets a Java 17+ backend (Spring examples assume Spring 6 / Spring Boot 3) with a browser-based
  UI. Requires Maven and a JDBC-accessible database. Playwright downloads browser binaries on first
  run. Build-tool integration supports Nx, Make, or plain Maven.
metadata:
  author: portable-acceptance-skills
  version: '1.0'
---

# Cucumber Acceptance-Test Setup

Stand up a complete acceptance-test harness in a project that has none. The harness drives the
running application through a real browser (Playwright), written as Cucumber scenarios, wired through
a Spring context, with database seeding/cleanup handled by SQL scripts.

The end state is a **four-layer architecture** — feature files → step definitions → domain
actors/assertions → drivers (UI page objects + API). This skill builds the *plumbing*. Writing the
tests themselves is covered by the companion **cucumber-acceptance-authoring** skill.

## How to use this skill

Work top to bottom. Each phase points at a reference file with the exact templates. Every template
uses placeholders you resolve in Phase 0 — never paste them verbatim.

| Placeholder | Meaning | Example |
| --- | --- | --- |
| `{{BASE_PACKAGE}}` | Root Java package for the test module | `com.example.acceptance` |
| `{{GROUP_ID}}` / `{{ARTIFACT_ID}}` | Maven coordinates | `com.example` / `acceptance-tests` |
| `{{MODULE_PATH}}` | Path of the test module in the repo | `apps/acceptance-tests` or `acceptance-tests` |
| `{{APP_BASE_URL}}` | Where the UI is served | `http://localhost:4200` |
| `{{JDBC_URL}}` / `{{DB_USER}}` / `{{DB_PASSWORD}}` | Test database connection | `jdbc:postgresql://localhost:5432/app_db` |
| `{{JDBC_DRIVER}}` / `{{JDBC_DEP}}` | Driver class + Maven dep | `org.postgresql.Driver` |

## Phase 0 — Detect the stack and resolve placeholders

Before writing anything, inspect the repo. See [detect-and-parameterise](references/detect-and-parameterise.md)
for the checks. In short:

- **Build tool:** `nx.json` or `project.json` present → Nx. A `Makefile` at the root → Make. Neither
  → plain Maven. A repo can have both Nx and Make — wire whichever the team actually uses (ask if
  unclear; you can wire both).
- **Module location:** match the repo's convention — an Nx monorepo usually puts apps under `apps/`;
  a plain Maven repo may want a top-level `acceptance-tests/` module or a Maven submodule.
- **Base package:** mirror the backend's package root (e.g. backend `com.example.app` → tests
  `com.example.acceptance`).
- **Database + UI URL:** find how the app is served locally and which DB it talks to. These become
  the env vars the harness reads.

Confirm the resolved values with the user if any are ambiguous — a wrong base package or DB URL
makes everything downstream fail.

## Phase 1 — Create the Maven module and directory skeleton

1. Create the module directory and `pom.xml` from [maven-module](references/maven-module.md) (copy
   `assets/pom.xml.template`, substitute coordinates + JDBC driver).
2. Create the source skeleton — the four layers plus infrastructure:

```
{{MODULE_PATH}}/src/test/
  java/{{BASE_PACKAGE_PATH}}/acceptance/
    config/      # EnvironmentConfig, DatabaseConfig, PropertiesConfig, AcceptanceCucumberContext
    run/         # RunAcceptanceTests (JUnit Suite)
    support/     # SqlScriptRunner, ResponseUtil
    hooks/       # GlobalTestDataHooks, ScreenshotHooks
    world/       # TestWorld
    drivers/
      ui/pages/  # Playwright page objects (added per feature later)
      api/       # HTTP drivers (added per feature later)
    domain/      # actors + assertions (added per feature later)
    stepdefs/    # step definitions (added per feature later)
  resources/
    features/                    # .feature files (added later)
    fixtures/sql/
      global-cleanup.sql
      global-sequence-init.sql
    application.properties
    cucumber.properties
    junit-platform.properties
  java/{{INFRA_PACKAGE_PATH}}/config/
    AcceptanceSpringConfig.java  # the @Configuration that owns Playwright + component scan
```

> Keep `AcceptanceSpringConfig` in a package the component scan does **not** re-scan into a cycle —
> the reference class scans `{{BASE_PACKAGE}}.acceptance`, so place the config just outside it (e.g.
> `{{BASE_PACKAGE}}.acceptanceinfra.config`) exactly as the template shows.

## Phase 2 — Drop in the infrastructure classes

These are the same for every project — only the package line changes. Create them from:

- [spring-and-runner](references/spring-and-runner.md) — `AcceptanceSpringConfig` (Playwright
  `Browser`/`BrowserContext`/`Page` beans, `@ScenarioScope`, headless flag, component scan),
  `RunAcceptanceTests` (JUnit Platform Suite with `GLUE`/`PLUGIN`), `AcceptanceCucumberContext`,
  `PropertiesConfig`, `EnvironmentConfig`, `DatabaseConfig`.
- [lifecycle-and-seeding](references/lifecycle-and-seeding.md) — `SqlScriptRunner` (runs a classpath
  SQL script with deadlock retry), `GlobalTestDataHooks` (clean-before-scenario at
  `order = Integer.MIN_VALUE`), `ScreenshotHooks` (attach a failure screenshot to the report on every
  failing scenario), `TestWorld` (`@ScenarioScope` shared state), and the `global-cleanup.sql` /
  `global-sequence-init.sql` conventions.

## Phase 3 — Wire the build tool

Add the run target(s) for whatever Phase 0 detected. Full recipes in
[build-tool-integration](references/build-tool-integration.md). **Most projects have no Nx** — Make or
plain Maven is a complete, self-sufficient setup on its own:

- **Make** (primary when there's no Nx) — `Makefile` targets (`accept-test TAG=…`,
  `accept-test-headed`, `accept-smoke`, `accept-compile`, `accept-build`, `accept-install`) wrapping
  the Maven commands. This is a full interface to the harness; Nx is not required for anything.
- **Plain Maven** — the underlying `mvn test -Dcucumber.filter.tags=…` invocation everything else
  delegates to; run it directly with no build tool at all.
- **Nx** (only if the repo already uses it) — a `project.json` with a `test` target running
  `mvn test -Dcucumber.filter.tags="{args.tag}"`, plus `build`/`clean`/`serve` and optional
  headed/headless env configurations.

All three funnel to the same Maven command, so the harness behaves identically however it is launched.
Wire the tag filter through in every case — scoping runs by `@story-<ID>` / `@slice-<ID>` tags is how
developers run one test during ATDD.

**Optional — isolated runs.** By default the harness runs against whatever stack you have up (your dev
app + dev DB). To run against a dedicated throwaway stack on its own ports and a fresh database
instead — so tests never touch your dev environment — add a `make accept-isolated` target backed by a
small Docker Compose file. See [isolated-testing](references/isolated-testing.md).

## Phase 4 — Verify with a smoke scenario

Prove the plumbing works before anyone writes a real scenario. Always run the module **from inside its
own directory** — it has no intra-repo Maven dependencies, so it builds standalone and needs no parent
reactor pom (see build-tool-integration).

1. **Compile:** `cd {{MODULE_PATH}} && mvn -q test-compile` (or `make accept-compile` / the Nx build
   target). This alone proves the Spring wiring, glue package, and imports resolve.
2. **Create a throwaway smoke scenario** — do this *before* the first `test` run. Cucumber's
   `@SelectClasspathResource("features")` needs at least one `.feature` file on the classpath: Maven
   does not copy an empty `features/` directory to `target/test-classes`, so a run against a truly
   empty features dir can fail to resolve the resource. Add:
   - `src/test/resources/features/smoke/smoke.feature`:
     ```gherkin
     @pre-push
     Feature: Harness smoke test
       Scenario: The app is reachable and the harness boots
         When the browser opens the application root
         Then the page has loaded
     ```
   - a matching step-def class in the glue package (`{{BASE_PACKAGE}}.acceptance.stepdefs`) that
     injects the `Page` bean and `EnvironmentConfig`, navigates to the **root** URL (root/login is the
     one place a direct navigation is allowed), and asserts the page loaded:
     ```java
     @When("the browser opens the application root")
     public void openRoot() { page.navigate(env.appBaseUrl()); }

     @Then("the page has loaded")
     public void pageLoaded() { assertThat(page.locator("body")).isVisible(); }
     ```
3. **Run it** against the running app, scoped by its tag:
   `cd {{MODULE_PATH}} && mvn clean test -Dcucumber.filter.tags="@pre-push"` (or `make accept-smoke`).
   Confirm the Spring context + Playwright beans initialise, the browser opens, and the scenario is
   green. Java Playwright downloads its browser binaries automatically on first run; if that is
   disabled in the environment, run the documented `playwright install` step (or `make accept-install`)
   once.
4. **Delete the smoke feature and step-def** once green (or keep it tagged `@pre-push` as the seed of
   your pre-push smoke set — see Phase 5).

**Reports.** Every run writes a self-contained HTML report to `{{MODULE_PATH}}/target/cucumber-report.html`
(plus a `cucumber-report.json` for tooling) — open the HTML in a browser to see each scenario, its
steps, timings, and any failure. This is configured by the `PLUGIN` list on `RunAcceptanceTests` and
mirrored in `cucumber.properties`; keep the two in sync (see spring-and-runner). When a scenario
fails, `ScreenshotHooks` embeds a full-page screenshot into the report automatically (installed in
Phase 2 — see lifecycle-and-seeding), so a failing run shows the browser state under the failed step.

Once green, hand off to **cucumber-acceptance-authoring** to write real scenarios.

## Phase 5 — (optional) Wire a git pre-push hook

If the team wants broken tests kept off the shared branch, add a pre-push hook — but tier it so it
stays fast and never blocks a push just because the developer's stack isn't running. See
[pre-push-hooks](references/pre-push-hooks.md). In short: always compile the module on pre-push (fast,
catches broken glue), run a `@pre-push`/`@smoke` tagged subset only when the app is actually
reachable, and leave the full suite to CI. The hook can call the Make targets (`make accept-compile`,
`make accept-smoke`) so there's one source of truth — no Nx required. Use Husky if the repo has Node
tooling (the hook is committed and every clone gets it), or a committed native hook via
`core.hooksPath` in a Node-free repo.

## References

- [detect-and-parameterise](references/detect-and-parameterise.md) — stack detection + placeholder table
- [maven-module](references/maven-module.md) — annotated `pom.xml` and dependency rationale
- [spring-and-runner](references/spring-and-runner.md) — Spring config, Suite runner, context, env/DB config
- [lifecycle-and-seeding](references/lifecycle-and-seeding.md) — SQL runner, global hooks, TestWorld, global SQL
- [build-tool-integration](references/build-tool-integration.md) — Nx, Make, and Maven run targets
- [isolated-testing](references/isolated-testing.md) — run against a throwaway stack (Docker Compose / Make / Nx)
- [pre-push-hooks](references/pre-push-hooks.md) — tiered git pre-push hook (Husky / native / Nx affected)
- `assets/` — copy-ready `pom.xml.template`, `global-cleanup.sql`, `global-sequence-init.sql`,
  `cucumber.properties`, `junit-platform.properties`, `application.properties`
