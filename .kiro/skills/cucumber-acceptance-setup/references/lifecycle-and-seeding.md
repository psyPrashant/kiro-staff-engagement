# Data lifecycle: SQL runner, global hooks, TestWorld

The harness runs against a **shared, non-empty** database. To stay deterministic it cleans test data
before every scenario and resets sequences so seeded IDs land in a known range. Three pieces make
this work: `SqlScriptRunner`, `GlobalTestDataHooks`, and the two global SQL scripts.

## SqlScriptRunner — run a classpath SQL script

```java
package {{BASE_PACKAGE}}.acceptance.support;

import java.sql.SQLException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;
import {{BASE_PACKAGE}}.acceptance.config.DatabaseConfig;

@Component
public class SqlScriptRunner {

  private final DriverManagerDataSource dataSource;

  public SqlScriptRunner(DatabaseConfig databaseConfig) {
    this.dataSource = new DriverManagerDataSource();
    this.dataSource.setDriverClassName("{{JDBC_DRIVER}}");
    this.dataSource.setUrl(databaseConfig.url());
    this.dataSource.setUsername(databaseConfig.username());
    this.dataSource.setPassword(databaseConfig.password());
  }

  public void runClasspathScript(String classpathLocation) {
    int maxAttempts = 3;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      try {
        runScript(classpathLocation);
        return;
      } catch (RuntimeException e) {
        if (!isDeadlock(e) || attempt == maxAttempts) {
          throw e;
        }
        sleepBeforeRetry(attempt);
      }
    }
  }

  private void runScript(String classpathLocation) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.addScript(new ClassPathResource(classpathLocation));
    populator.setContinueOnError(false);
    populator.execute(dataSource);
  }

  private boolean isDeadlock(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof SQLException sql && "40P01".equals(sql.getSQLState())) {
        return true; // 40P01 is Postgres deadlock; adjust the SQLState for other databases
      }
      if (current.getMessage() != null && current.getMessage().toLowerCase().contains("deadlock")) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private void sleepBeforeRetry(int attempt) {
    try {
      Thread.sleep(100L * attempt);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while retrying SQL script after deadlock", e);
    }
  }
}
```

The deadlock retry matters because cleanup can race in-flight API calls from a prior scenario's
browser. `40P01` is PostgreSQL's deadlock SQLState — change it for another database.

## GlobalTestDataHooks — the single global lifecycle owner

```java
package {{BASE_PACKAGE}}.acceptance.hooks;

import io.cucumber.java.Before;
import {{BASE_PACKAGE}}.acceptance.support.SqlScriptRunner;

public class GlobalTestDataHooks {

  private final SqlScriptRunner sqlScriptRunner;

  public GlobalTestDataHooks(SqlScriptRunner sqlScriptRunner) {
    this.sqlScriptRunner = sqlScriptRunner;
  }

  @Before(order = Integer.MIN_VALUE)
  public void globalSetup() {
    // Clean BEFORE the next scenario, not after the previous one: the browser can still have
    // in-flight API calls during teardown, which would race a post-scenario cleanup.
    sqlScriptRunner.runClasspathScript("fixtures/sql/global-cleanup.sql");
    sqlScriptRunner.runClasspathScript("fixtures/sql/global-sequence-init.sql");
  }
}
```

Rules that keep this reliable:

- This is the **only** hook that touches the **data lifecycle**. `order = Integer.MIN_VALUE`
  guarantees it runs before any story-scoped seed hook.
- Clean *before* each scenario (not after) — see the comment above.
- Any per-story seed hooks added later must be **tag-scoped** (`@Before("@story-<ID>")`) and run at a
  higher order so the global clean happens first.
- A read-only `@After` that only captures a screenshot (below) does **not** participate in the data
  lifecycle, so it lives in its own class rather than here — keeping data-cleanup and diagnostics
  separate.

## ScreenshotHooks — attach a screenshot to the report on failure

When a scenario fails, a screenshot of the browser at the moment of failure is the single most useful
piece of debugging information. Install this once at setup so **every** failing scenario attaches one
to the HTML report automatically — no per-scenario wiring needed.

```java
package {{BASE_PACKAGE}}.acceptance.hooks;

import com.microsoft.playwright.Page;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;

public class ScreenshotHooks {

  private final Page page;

  public ScreenshotHooks(Page page) {
    this.page = page;
  }

  @After
  public void captureScreenshotOnFailure(Scenario scenario) {
    if (scenario.isFailed()) {
      // The @ScenarioScope Page bean is still alive here — it is closed only when the scenario
      // scope ends, after all @After hooks run — so a full-page screenshot is safe to take.
      byte[] screenshot = page.screenshot(
        new Page.ScreenshotOptions().setFullPage(true)
      );
      scenario.attach(screenshot, "image/png", "failure-" + scenario.getName());
    }
  }
}
```

How it works and why it's safe:

- **Only on failure.** `scenario.isFailed()` gates it, so passing runs cost nothing.
- **`scenario.attach(...)`** embeds the PNG into the Cucumber HTML report (base64), so the image
  travels with the report — no separate files to manage. It shows inline under the failed scenario.
- **Global, not tag-scoped.** An unscoped `@After` means it covers every scenario from day one; you
  never remember to add it per story. It is read-only (it just reads the page), so it doesn't clash
  with the data-lifecycle rule above — that rule governs *data* setup/cleanup ordering, which this
  hook has nothing to do with.
- **Plain glue class**, like `GlobalTestDataHooks` — Cucumber instantiates it and resolves the `Page`
  bean from the Spring context. It just needs to live in the glue package (`{{BASE_PACKAGE}}.acceptance…`).

> The screenshots only appear if the run produces an HTML report — which the harness does by default
> (`html:target/cucumber-report.html`, see [spring-and-runner](spring-and-runner.md)). Open that file
> after a failing run and the screenshot is embedded under the failed step.

## Global SQL scripts

`fixtures/sql/global-cleanup.sql` (copy `assets/global-cleanup.sql`) deletes every test-created row.
The convention: all test data uses IDs above a high watermark (e.g. `900000`), so cleanup is a set of
`DELETE ... WHERE id > 900000` statements in **foreign-key-safe order** (leaf/child tables first,
parents last). You fill in the table list for the target schema — the template shows the shape:

```sql
-- Delete child rows before parents. Extend for your schema.
DELETE FROM order_line WHERE id > 900000;
DELETE FROM orders     WHERE id > 900000;
DELETE FROM customer   WHERE id > 900000;
```

`fixtures/sql/global-sequence-init.sql` (copy `assets/global-sequence-init.sql`) resets each
sequence so `DEFAULT`-generated IDs in seed inserts start at a known value (e.g. `990001`),
guaranteeing seeded rows fall inside the `> 900000` cleanup range:

```sql
-- PostgreSQL syntax. For MySQL use ALTER TABLE ... AUTO_INCREMENT = 990001;
ALTER SEQUENCE order_id_seq    RESTART WITH 990001;
ALTER SEQUENCE customer_id_seq RESTART WITH 990001;
```

Sequence-reset syntax is database-specific — Postgres uses `ALTER SEQUENCE ... RESTART`, MySQL uses
`ALTER TABLE ... AUTO_INCREMENT`, others differ. Adjust to the target DB.

> Why a high watermark? It cleanly separates disposable test data from real/reference data that must
> survive. Pick a watermark comfortably above any real ID the app will generate.

## TestWorld — scenario-scoped shared state

```java
package {{BASE_PACKAGE}}.acceptance.world;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class TestWorld {

  // One field per piece of state a scenario carries between steps.
  // Start empty; add fields as scenarios need them (see cucumber-acceptance-authoring).
  private String lastCreatedEntityName;

  public String getLastCreatedEntityName() {
    return lastCreatedEntityName;
  }

  public void setLastCreatedEntityName(String name) {
    this.lastCreatedEntityName = name;
  }
}
```

`@ScenarioScope` means one fresh instance per scenario. Never share state between step-definition
classes via static fields — inject `TestWorld` instead. This is how a `When` step records a name and
a later `Then` step reads it.

## Register `GlobalTestDataHooks` in the glue

`GlobalTestDataHooks` is a plain class (not `@Component`) — Cucumber instantiates glue classes itself
and resolves their constructor args from the Spring context via `cucumber-spring`. It only needs to
live in the glue package (`{{BASE_PACKAGE}}.acceptance...`) to be picked up.
