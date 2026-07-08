# Gherkin and the domain DSL

## The DSL describes what the business wants, not how the tech works

**Prefer**
- `the provider publishes a draft widget`
- `the consumer requests access to a widget`
- `the widget appears in the catalogue`

**Avoid**
- `POST /api/widgets/publish`
- `click the publish button`
- `insert a row into the widget table`
- `call widgetService.publishDraft()`

Good DSL is: domain terms, readable aloud, stable across infrastructure changes, meaningful to
non-technical people, and reusable across scenarios.

**Extend the DSL only** when a new business verb or concept appears, or a repeated awkward phrase
needs simplifying. Do not extend it for one-off technical shortcuts.

## Always `Scenario Outline` + `Examples`

Every scenario is a `Scenario Outline` with an `Examples` table — even with a single row. Test data
never goes inline into step text; it goes in the table as `<placeholder>` values.

**Correct:**
```gherkin
@epic-WIDG @story-WIDG-2 @slice-WIDG-2.1
Scenario Outline: [WIDG-2.1] User creates a draft widget with core details
  Given a user signed in as "<email>" with password "<password>"
  And the active team is "<team_code>"
  When the user creates a widget named "<widget_name>"
  Then the widget detail page shows "<widget_name>" in draft status

  Examples:
    | email            | password     | team_code | widget_name             |
    | user@example.com | password123! | ALPHA     | ACCEPTANCE_TEST WIDGET  |
```

**Wrong** — data inlined, no outline:
```gherkin
Scenario: User creates a draft widget
  Given a user signed in as "user@example.com" with password "password123!"
  When the user creates a widget named "ACCEPTANCE_TEST WIDGET"
  Then the widget detail page shows the widget in draft status
```

## Tags — three levels

Every scenario carries all three tag levels so hooks and runs can scope precisely:

| Tag | Format | Example |
| --- | --- | --- |
| Epic | `@epic-<EPIC>` | `@epic-WIDG` |
| Story | `@story-<EPIC>-<N>` | `@story-WIDG-2` |
| Slice | `@slice-<EPIC>-<N>.<S>` | `@slice-WIDG-2.1` |

The `@story-*` tag is the primary filter key and must exactly match the Backlog ID used in the commit
message. The **epic prefix** is the alphabetic part of the Backlog ID (`WIDG` from `WIDG-2`) — never a
descriptive word like `WIDGET`.

## Backlog ID — the same ID in four places

```
[<EPIC>-<N>]
```

All four must match:
- commit message prefix: `[<EPIC>-<N>] <description>`
- Gherkin tag: `@story-<EPIC>-<N>`
- feature file name: `<EPIC>-<N>-<description>.feature`
- scenario name prefix: `[<EPIC>-<N>.<S>] <behaviour>`

> If your tracker uses a separate ticket key (e.g. `PROJ-512`), that is **not** the Backlog ID. Pick
> one Backlog ID scheme and use it consistently across all four places.

## Scenario naming — slice ID first

Every scenario name begins with the slice ID in square brackets, then a plain-English behaviour:

```gherkin
@slice-WIDG-2.1
Scenario Outline: [WIDG-2.1] User creates a draft widget with core details
```

Why: test runners, CI logs, and HTML reports print the scenario name verbatim. Leading with
`[<EPIC>-<N>.<S>]` makes it trivial to see which slice failed without opening the file. The bracketed
ID must match the `@slice-` tag on the same scenario.

## Feature file location

```
resources/features/<domain>/<EPIC>-<N>-<kebab-description>.feature
```

Example: `resources/features/widget/WIDG-2-create-widget.feature`. The domain folder matches the
business area, not the code package. If a file for the feature's Backlog ID already exists, add the
new scenario to it rather than creating a second file.

## Structure discipline

- One behaviour per scenario. Avoid long `And` chains — refactor into a single expressive step if you
  need more than ~4 consecutive `And`s.
- Use `Background` only for setup that genuinely applies to every scenario in the file.
- Steps describe *what* the user does or sees, never *how* the system implements it.

## Step reuse — extend before adding

Before writing a new `@Given`/`@When`/`@Then`, search existing step definitions for one that expresses
the same intent and reuse it — even if it needs a small tweak (e.g. hardcoded string → `{string}`
parameter). Only create a new step when nothing covers the intent. Duplicate step definitions cause
ambiguous-step failures.
