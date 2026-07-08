# Full worked example

One acceptance criterion carried through all four layers, using the neutral `widget` domain and base
package `com.example.acceptance`. Substitute your real domain and package.

**AC:** *As a provider, I want to create a draft widget so that I can prepare it before publishing.*
Backlog ID `WIDG-2`, first slice `WIDG-2.1`.

## Layer 1 — Feature file

`resources/features/widget/WIDG-2-create-widget.feature`

```gherkin
@epic-WIDG @story-WIDG-2
Feature: WIDG-2 Create a draft widget

  @slice-WIDG-2.1
  Scenario Outline: [WIDG-2.1] Provider creates a draft widget with core details
    Given a provider signed in as "<email>" with password "<password>"
    And the active team is "<team_code>"
    When the provider creates a widget named "<widget_name>"
    Then the widget detail page shows "<widget_name>" in draft status

    Examples:
      | email            | password     | team_code | widget_name            |
      | user@example.com | password123! | ALPHA     | ACCEPTANCE_TEST WIDGET |
```

Note: `Scenario Outline` + `Examples` even for one row; slice ID in the scenario name matches the
`@slice-` tag; prerequisite state (a signed-in provider on a team) is established through `Given`
steps that drive the real sign-in/team-switch UI — no seed.

## Layer 2 — Step definitions

`stepdefs/widget/CreateWidgetSteps.java`

```java
package com.example.acceptance.stepdefs.widget;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import com.example.acceptance.domain.auth.AuthActor;
import com.example.acceptance.domain.widget.WidgetActor;
import com.example.acceptance.domain.widget.WidgetAssertions;

public class CreateWidgetSteps {

  private final AuthActor authActor;
  private final WidgetActor widgetActor;
  private final WidgetAssertions widgetAssertions;

  public CreateWidgetSteps(AuthActor authActor, WidgetActor widgetActor, WidgetAssertions widgetAssertions) {
    this.authActor = authActor;
    this.widgetActor = widgetActor;
    this.widgetAssertions = widgetAssertions;
  }

  @Given("a provider signed in as {string} with password {string}")
  public void aProviderSignedInAs(String email, String password) {
    authActor.signIn(email, password);
  }

  @Given("the active team is {string}")
  public void theActiveTeamIs(String teamCode) {
    authActor.switchTeam(teamCode);
  }

  @When("the provider creates a widget named {string}")
  public void theProviderCreatesAWidgetNamed(String name) {
    widgetActor.createWidget(name);
  }

  @Then("the widget detail page shows {string} in draft status")
  public void theWidgetDetailPageShowsInDraftStatus(String name) {
    widgetAssertions.detailShowsDraftWidget(name);
  }
}
```

Reuse `AuthActor.signIn` / `switchTeam` if they already exist — don't re-declare those steps.

## Layer 3 — Domain actor and assertions

`domain/widget/WidgetActor.java`

```java
package com.example.acceptance.domain.widget;

import org.springframework.stereotype.Component;
import com.example.acceptance.drivers.ui.pages.WidgetListPage;
import com.example.acceptance.drivers.ui.pages.WidgetCreatePage;
import com.example.acceptance.world.TestWorld;

@Component
public class WidgetActor {

  private final WidgetListPage listPage;
  private final WidgetCreatePage createPage;
  private final TestWorld world;

  public WidgetActor(WidgetListPage listPage, WidgetCreatePage createPage, TestWorld world) {
    this.listPage = listPage;
    this.createPage = createPage;
    this.world = world;
  }

  public void createWidget(String name) {
    listPage.openFromNav();     // click the nav link, not a URL
    listPage.startCreate();
    createPage.fillName(name);
    createPage.submit();
    world.setLastCreatedWidgetName(name);
  }
}
```

`domain/widget/WidgetAssertions.java`

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

  public void detailShowsDraftWidget(String name) {
    detailPage.assertNameVisible(name);
    detailPage.assertStatus("Draft");
  }
}
```

## Layer 4 — Page objects

`drivers/ui/pages/WidgetCreatePage.java`

```java
package com.example.acceptance.drivers.ui.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.springframework.stereotype.Component;

import static com.example.acceptance.support.ResponseUtil.waitForResponse;

@Component
public class WidgetCreatePage {

  private final Page page;

  public WidgetCreatePage(Page page) {
    this.page = page;
  }

  public void fillName(String name) {
    page.getByTestId("widget-name-input").fill(name);
  }

  public void submit() {
    // wait for the create call so the detail assertion doesn't race persistence
    waitForResponse(page, "POST", "**/api/widgets", () ->
      page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click());
  }
}
```

`drivers/ui/pages/WidgetDetailPage.java`

```java
package com.example.acceptance.drivers.ui.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Page;
import org.springframework.stereotype.Component;

@Component
public class WidgetDetailPage {

  private final Page page;

  public WidgetDetailPage(Page page) {
    this.page = page;
  }

  public void assertNameVisible(String name) {
    assertThat(page.getByTestId("widget-title")).hasText(name);
  }

  public void assertStatus(String status) {
    assertThat(page.getByTestId("widget-status")).containsText(status);
  }
}
```

## Run it and watch it fail

```bash
# Nx
nx test acceptance-tests --tag="@slice-WIDG-2.1"
# Make
make accept-test TAG="@slice-WIDG-2.1"
# plain Maven
mvn -pl <module> clean test -Dcucumber.filter.tags="@slice-WIDG-2.1"
```

Confirm, before writing any production code:
1. The test runs (no `UNDEFINED`/ambiguous steps).
2. It fails.
3. It fails for the **expected** reason (the create feature doesn't exist yet), not a wiring error.
4. The failure message is understandable — improve it if not.

Only then implement the feature and drive the test to green.
