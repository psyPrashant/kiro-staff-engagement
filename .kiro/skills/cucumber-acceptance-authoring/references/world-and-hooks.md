# TestWorld and hooks

Substitute your base package for `com.example`.

## TestWorld — scenario-scoped shared state

```java
package com.example.acceptance.world;

import io.cucumber.spring.ScenarioScope;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class TestWorld {

  private String lastCreatedWidgetName;
  private String activeTeamCode;

  public String getLastCreatedWidgetName() { return lastCreatedWidgetName; }
  public void setLastCreatedWidgetName(String v) { this.lastCreatedWidgetName = v; }

  public String getActiveTeamCode() { return activeTeamCode; }
  public void setActiveTeamCode(String v) { this.activeTeamCode = v; }
}
```

Created fresh per scenario, destroyed after. Inject it wherever state must pass between step-
definition classes within one scenario. **Never** use static fields for this — they bleed between
scenarios and cause order-dependent flakes.

### Adding a field
1. Add the field with a getter/setter.
2. Inject `TestWorld` into the step-def (or actor) class that sets it.
3. Set it in a `@Given`/`@When` step that establishes the state.
4. Read it in downstream steps that need it.

Prefer small typed records over loose strings when a piece of state has structure, e.g.
`record SeededWidget(String name) {}` stored as one field.

## Global lifecycle — one owner

`GlobalTestDataHooks` (from the harness) is the **single** unscoped hook owner. It runs at
`@Before(order = Integer.MIN_VALUE)`, cleaning test data and resetting sequences before every
scenario:

```java
@Before(order = Integer.MIN_VALUE)
public void globalSetup() {
  sqlScriptRunner.runClasspathScript("fixtures/sql/global-cleanup.sql");
  sqlScriptRunner.runClasspathScript("fixtures/sql/global-sequence-init.sql");
}
```

- `global-cleanup.sql` deletes all rows above the high watermark (e.g. `id > 900000`) in FK-safe order.
- `global-sequence-init.sql` resets sequences so seeded IDs land in that range.
- **Never add a second unscoped `@Before`/`@After`.** This is the only one. If you genuinely need a
  global step after it, use `order = Integer.MIN_VALUE + 1`.

## Slice/story seed hooks — tag-scoped, only when blocked

Most slices need **no** seed hook — build prerequisite state through the UI (see
[seeding-conventions](seeding-conventions.md)). When a prerequisite is genuinely blocked, add a
tag-scoped `@Before` that seeds data only:

```java
// one slice
@Before("@slice-WIDG-3.1")
public void seedWidg31() {
  sqlScriptRunner.runClasspathScript("fixtures/sql/widget/WIDG-3.1-seed.sql");
}

// several slices sharing one seed file
@Before("@slice-WIDG-3.1 or @slice-WIDG-3.3")
public void seedWidg3() {
  sqlScriptRunner.runClasspathScript("fixtures/sql/widget/WIDG-3-seed.sql");
}
```

Rules:
- Scope every slice hook with `value = "@story-<ID>"` or `"@slice-<ID>"`.
- Slice `@Before` seeds data **only** — never call cleanup inside it (the global hook owns cleanup).
- **Never write a per-slice cleanup SQL file** — `global-cleanup.sql` cleans everything above the
  watermark before and effectively across scenarios.
- **Don't add a per-slice screenshot hook.** Failure screenshots are handled globally by the
  harness's `ScreenshotHooks` (installed at setup) — every failing scenario already attaches a
  full-page screenshot to the HTML report. Adding a slice-scoped one would just double the attachment.

## Execution order per scenario

```
@Before(order = MIN_VALUE)   GlobalTestDataHooks.globalSetup()   ← clean + reset sequences
@Before("@slice-...")        seedXxx()                            ← story/slice seed (only if blocked)
--- scenario steps run ---
@After                       ScreenshotHooks (global)             ← screenshot on failure only
```
