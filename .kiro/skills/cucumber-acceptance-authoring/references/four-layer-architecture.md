# Four-layer architecture

Package root (example): `com.example.acceptance`. Substitute your project's base package.

Each layer has one job. Data flows down (feature → step → domain → driver); nothing skips a layer.

```
Layer 1  Feature files        resources/features/{domain}/       Gherkin, business language only
Layer 2  Step definitions     stepdefs/{domain}/                 Wire Gherkin → domain; 1–2 lines/step
Layer 3  Domain actors/       domain/{domain}/                   Orchestrate drivers; domain language
         assertions
Layer 4  Drivers              drivers/ui/pages/ , drivers/api/   All Playwright & HTTP calls
```

## Layer 1 — Feature files

Gherkin in business language. No class references, field names, selectors, or URLs. One feature file
per domain area, named by Backlog ID. Scenarios carry `@epic`/`@story`/`@slice` tags that scope which
hooks fire. See [gherkin-and-dsl](gherkin-and-dsl.md).

## Layer 2 — Step definitions

```java
package com.example.acceptance.stepdefs.widget;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import com.example.acceptance.domain.widget.WidgetActor;
import com.example.acceptance.domain.widget.WidgetAssertions;

public class WidgetManagementSteps {

  private final WidgetActor actor;
  private final WidgetAssertions assertions;

  public WidgetManagementSteps(WidgetActor actor, WidgetAssertions assertions) {
    this.actor = actor;
    this.assertions = assertions;
  }

  @When("the user creates a widget named {string}")
  public void theUserCreatesAWidgetNamed(String name) {
    actor.createWidget(name);
  }

  @Then("the widget detail page shows {string}")
  public void theWidgetDetailPageShows(String name) {
    assertions.detailPageShowsWidget(name);
  }
}
```

Rules:
- Constructor-inject the actor and assertions objects. `cucumber-spring` resolves them from the
  Spring context.
- One or two lines per step — delegate immediately. No logic, no Playwright, no HTTP here.
- Match the annotation style of sibling steps in the same file for readability (Cucumber treats
  `Given`/`When`/`Then`/`And` identically, but consistency helps maintenance).

## Layer 3 — Domain actors and assertions

Actors perform the user's actions; assertions verify outcomes. Both are `@Component` beans.

```java
package com.example.acceptance.domain.widget;

import org.springframework.stereotype.Component;
import com.example.acceptance.drivers.ui.pages.WidgetListPage;
import com.example.acceptance.drivers.ui.pages.WidgetDetailPage;
import com.example.acceptance.world.TestWorld;

@Component
public class WidgetActor {

  private final WidgetListPage listPage;
  private final WidgetDetailPage detailPage;
  private final TestWorld world;

  public WidgetActor(WidgetListPage listPage, WidgetDetailPage detailPage, TestWorld world) {
    this.listPage = listPage;
    this.detailPage = detailPage;
    this.world = world;
  }

  public void createWidget(String name) {
    listPage.openFromNav();          // reach the page by clicking, never by URL
    listPage.startCreate();
    detailPage.fillName(name);
    detailPage.submit();
    world.setLastCreatedWidgetName(name);   // record state for later steps
  }
}
```

```java
package com.example.acceptance.domain.widget;

import org.springframework.stereotype.Component;
import com.example.acceptance.drivers.ui.pages.WidgetDetailPage;

@Component
public class WidgetAssertions {

  private final WidgetDetailPage detailPage;

  public WidgetAssertions(WidgetDetailPage detailPage) {
    this.detailPage = detailPage;
  }

  public void detailPageShowsWidget(String name) {
    detailPage.assertNameVisible(name);   // delegates the actual assertThat to the page object
  }
}
```

Rules:
- Actors: actions only (navigate, fill, click, submit). Assertions: verify only.
- Method names use domain language (`createWidget`, `detailPageShowsWidget`).
- Both delegate to page objects (Layer 4) for the mechanics.

### Reusing an assertion helper — audit for hidden negative checks

When reusing a domain helper, read every line — especially `*Absent` / `hasCount(0)` / `not(...)` /
`isHidden()` checks. Those negative assertions encode an assumption about **which persona** the helper
was written for. Reusing a "regular user shouldn't see admin links" helper in an admin scenario will
fail at runtime even though a dry-run shows everything wired. If your persona violates an embedded
assumption, narrow the helper to positive-only checks (moving negatives to explicit Gherkin) or write
a persona-specific helper. Never silently let conflicting absences ride.

## Layer 4 — Drivers

**UI page objects** (`drivers/ui/pages/`) contain all Playwright calls. See
[page-object-patterns](page-object-patterns.md).

**API driver** (`drivers/api/`) makes HTTP calls to verify backend state independently of the UI, or
as a last-resort setup path when no UI flow exists yet. It is not a routine setup mechanism — prefer
driving the real UI flow.

## Spring wiring

All classes in all layers are `@Component` beans discovered by the same component scan
(`com.example.acceptance`). `TestWorld` is `@ScenarioScope`, so it is created fresh per scenario and
injected wherever state must be shared between step-definition classes.
