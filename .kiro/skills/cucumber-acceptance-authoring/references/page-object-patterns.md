# Page object patterns

All Playwright calls live in page objects under `drivers/ui/pages/`. Substitute your base package for
`com.example`.

## Locator priority

1. `page.getByTestId("...")` — primary. Add `data-testid` attributes to the UI templates.
2. `page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save"))` — for interactive
   elements without a test ID.
3. `page.locator("css")` — last resort only.

## Anatomy

```java
package com.example.acceptance.drivers.ui.pages;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.springframework.stereotype.Component;

@Component
public class WidgetDetailPage {

  private final Page page;

  public WidgetDetailPage(Page page) {
    this.page = page;
  }

  // Navigation — reach this page by clicking through the UI, identifying the target by content
  // so it stays correct against a large, shared, paginated list. Never navigate by URL.
  public void openFromList(String widgetName) {
    page.getByTestId("widget-list-item")
        .filter(new Locator.FilterOptions().setHasText(widgetName))
        .getByRole(AriaRole.LINK)
        .click();
  }

  // Actions — no assertions here.
  public void fillName(String name) {
    page.getByTestId("widget-name-input").fill(name);
  }

  public void submit() {
    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save")).click();
  }

  // Assertion methods — assertThat only; called from the domain Assertions class.
  public void assertNameVisible(String name) {
    assertThat(page.getByTestId("widget-title")).hasText(name);
  }
}
```

Rules:
- Action methods contain no assertions; assertion methods contain only `assertThat(...)`.
- Method names describe UI mechanics (`assertNameVisible`), not business meaning
  (`assertWidgetWasCreated` belongs in the domain layer).
- **No `page.navigate()` to deep application URLs.** Reach pages by clicking real links/menus/buttons.
  Only the base URL (root/login) may be navigated directly, and only when there is no UI entry point
  yet. If the navigation path to a page is itself not yet implemented, a temporary direct navigation
  to that one page is allowed — note it and remove it once the navigation ships.

## `assertThat` auto-waits

```java
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

assertThat(page.getByTestId("title")).isVisible();
assertThat(page.getByTestId("error")).not().isVisible();
assertThat(page.getByTestId("title")).hasText("My Widget");
assertThat(page.getByTestId("status")).containsText("Active");
```

`assertThat(...).isVisible()` auto-waits — explicit `waitFor` calls are rarely needed, and
`Thread.sleep` never is. Never assert with raw JUnit `assertEquals` on DOM content; it does not wait
and will flake.

## Waiting for a mutating response

When a UI action triggers a mutating HTTP call and the test asserts state after it persists, wait for
the response so the assertion doesn't race the network. A small shared helper (e.g.
`support/ResponseUtil.waitForResponse(page, method, urlPattern, action)`) keeps this consistent:

```java
waitForResponse(page, "POST", "**/api/widgets", () ->
  page.getByTestId("save-button").click());
```

- Use for POST/PUT/PATCH/DELETE where the test asserts after persistence.
- `urlPattern` uses glob syntax (`*` one segment, `**` many).
- For plain GET loads, `assertThat(...).isVisible()` auto-wait is enough — no response wait needed.

## Never embed an index in a test ID

A `data-testid` identifies **what** an element is, never **which one**.

**Wrong:**
```html
<li data-testid="widget-list-item-{{ i }}">...</li>
```
```java
page.getByTestId("widget-list-item-1")
```

**Right — structural test ID, resolve the specific element by content:**
```html
<li data-testid="widget-list-item">...</li>
```
```java
page.getByTestId("widget-list-item")
    .filter(new Locator.FilterOptions().setHasText(widgetName))
```

Positional selection (`.first()`, `.last()`, `.nth(n)`) is a **last resort**, allowed only when the
list is already narrowed to a single deterministic match (e.g. after filtering to a unique value) or
when position is itself the behaviour under test (e.g. sort order). Against a shared, paginated,
lazy-loading list, `.first()` can match a stranger's row or miss a target that hasn't rendered yet.

## Paginated / lazy lists

Don't assume your record is on the first page or already in the DOM. Search/filter to it, or
page/scroll until it is present, before locating. A reusable "find across pages" helper that clicks
the next-page control until the filtered row appears keeps this out of individual page objects. The
next-page control selector is framework-specific (e.g. a component library's paginator button) — keep
it in one place.

## `data-testid` is a contract with the frontend

A page object can only find `page.getByTestId("save-button")` if the frontend actually renders
`data-testid="save-button"`. Those attributes live in **frontend production code** — the Angular
component templates (`.html`) of the feature under test. They are part of delivering the slice, not
an afterthought:

- **When a test ID you need already exists**, reuse it — grep the templates for `data-testid` first so
  you match the app's existing names rather than inventing a parallel set.
- **When it's missing**, add it to the component template as part of the same slice/commit that makes
  the test pass. A `data-testid` is a stable, intentional test hook; adding one is a safe,
  non-visual change. Prefer it over coupling the test to CSS classes or text that will drift.
- **If you genuinely cannot touch the frontend** (not your codebase, or the component isn't built
  yet), flag the missing test IDs in your summary so whoever owns that code adds them — don't paper
  over it with a brittle CSS/text locator.

### Convention

```html
<button data-testid="save-button" (click)="save()">Save</button>

@for (widget of widgets(); track widget.id) {
  <li data-testid="widget-list-item">{{ widget.name }}</li>
}
```

- **kebab-case**, named by the element's role in the UI (`save-button`, `widget-list-item`,
  `widget-status`) — never by position or index.
- One structural test ID per *kind* of element; resolve the specific instance by content
  (`.filter(setHasText(...))`), as in the "never embed an index" section above.
- Put the test ID on the element the test interacts with or asserts on — the clickable control, the
  row container, the field input, the status badge — not a wrapper three levels up.
- Keep names stable. A `data-testid` renamed on a whim breaks every page object that used it; treat it
  like the public API it is.
