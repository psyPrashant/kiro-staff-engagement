---
name: cucumber-acceptance-authoring
description: >
  Write failing-first, non-flaky Cucumber acceptance tests for a Java + web-UI project that already
  has the four-layer Cucumber + Playwright + Spring harness in place. Use this skill whenever you are
  authoring or editing anything under the acceptance-test module: Gherkin feature files, Java step
  definitions, domain actors/assertions, Playwright page objects, API drivers, hooks, TestWorld
  state, or seed SQL. It encodes the ATDD discipline (write the failing test before the code), the
  four-layer architecture, the domain DSL, UI-driven prerequisite setup, seeding-as-a-last-resort,
  and the anti-flakiness rules for shared/paginated databases. Triggers on: "write the acceptance
  test", "write the failing test", "create the feature file", "add a scenario", "add a page object",
  "store something in TestWorld", "add seed data", or any task touching the acceptance-test module.
  To stand up the harness in a project that has none, use cucumber-acceptance-setup first.
compatibility: >
  Assumes the four-layer Cucumber + Playwright + Spring harness already exists (as produced by the
  cucumber-acceptance-setup skill): a Spring context owning a scenario-scoped Playwright Page, a
  JUnit Platform Suite runner, a TestWorld bean, and global data-cleanup hooks.
metadata:
  author: portable-acceptance-skills
  version: '1.0'
---

# Cucumber Acceptance-Test Authoring

Turn a selected acceptance criterion into a **failing executable specification** before the feature is
implemented, wired through the four-layer harness and exercised through the app the way a real user
would. This skill is about *what* to write and *why*; the harness plumbing is assumed to exist.

Throughout, the running example uses a neutral `widget` domain and epic prefix `WIDG`. Substitute
your project's real domain, epics, roles, and URLs.

## Core philosophy — test the app, not the seed

The test must exercise real application behaviour, not verify that a row you inserted renders. A test
that seeds a widget and asserts it appears in the list proves the seed rendered — not that the create
flow works. So:

- **Establish prerequisite state by driving the app's real UI flow.** Need a widget to edit? Create
  it through the UI first, in a `Given`/`Background` step, using the same actor/page-object methods
  the scenario itself uses. This is the default.
- **Seed SQL is the exception.** Reach for a seed *only* when the prerequisite feature is blocked —
  not implemented yet, un-createable through the app, or genuinely the only viable way (see
  [seeding-conventions](references/seeding-conventions.md)).
- **An API/HTTP driver** is for *verifying* backend state independently of the UI, and as a
  last-resort setup path — not a routine way to set up data.
- **Navigate by clicking real links, menus, and buttons — never by typing a URL** (except the
  root/login entry point, or when the navigation itself is not yet implemented).
- **Never assume a clean database.** Tests run against shared environments with many pre-existing
  records; design for pagination, lazy-loading, and filtering by content.
- **Preserve integrity.** The test must still fail if the feature regresses, and must assert exactly
  what the Gherkin says. Never weaken a test to force it green.

**Setup order of preference:** drive the real UI flow → seed SQL only when blocked → API driver for
verification / last resort.

## Phase 1 — Write the failing test

### Inputs
- Backlog ID (e.g. `WIDG-3`) and the selected acceptance criterion
- Any story/slice plan
- Existing feature files and step definitions

### Output
- One failing acceptance test for the first slice
- Notes about missing drivers/support code
- A short explanation of *why* the failure is the expected one

### Must do
- Use domain language a non-technical stakeholder can read (see [gherkin-and-dsl](references/gherkin-and-dsl.md)).
- Include Backlog ID traceability in three places: the `@story`/`@slice` tags, the feature file name,
  and the scenario-name prefix `[<EPIC>-<N>.<S>]`.
- Always use `Scenario Outline` + `Examples` — even for a single data row. Never inline test data
  into step text.
- Search existing step definitions and reuse them before writing new ones.
- Establish prerequisite state by driving the real UI flow in `Given`/`Background` steps; seed only
  when the prerequisite feature is blocked.
- Reach the feature by clicking real links/menus/buttons — never a deep URL (except root/login).
- Start with the simplest success case and make the test readable first.
- Run it and observe it fail **for the expected reason** before implementing anything.

### Must not do
- Write the test against controller/DB/API internals.
- Combine multiple business topics in one scenario.
- Inline test data into step text — it belongs in the `Examples` table.
- Duplicate a step that already exists — generalise it instead.

## Phase 2 — Wire the four layers

Keep each layer to its job (full map in [four-layer-architecture](references/four-layer-architecture.md)):

| Layer | Location | Responsibility |
| --- | --- | --- |
| **1 — Feature files** | `resources/features/{domain}/` | Gherkin in business language; no technical detail |
| **2 — Step definitions** | `stepdefs/{domain}/` | Wire Gherkin to the domain layer; one or two lines per step |
| **3 — Domain actors/assertions** | `domain/{domain}/` | Orchestrate drivers; express intent in domain language |
| **4 — Drivers** | `drivers/ui/pages/` and `drivers/api/` | All Playwright and HTTP calls |

Key rules:
- Step defs are thin — delegate to an actor or assertions object; no Playwright or HTTP calls in them.
- All Playwright calls live in page objects; all HTTP calls live in the API driver.
- Share state within a scenario via `TestWorld`, never static fields (see [world-and-hooks](references/world-and-hooks.md)).
- Use `getByTestId()` as the primary locator; `getByRole()` for interactive elements without a test
  ID. The `data-testid`s live in the frontend's production templates — reuse the ones already there,
  and add any missing ones to the component as part of the same slice (they're a stable test contract,
  not a CSS class). Assert with Playwright's auto-waiting `assertThat()` — never raw `assertEquals` on
  DOM content (see [page-object-patterns](references/page-object-patterns.md)).
- Tag any scenario that asserts on email with `@requires-email` and poll with Awaitility — email is
  async (see [email-testing](references/email-testing.md)).

## Phase 3 — Establish prerequisite state

Work down this list; stop at the first that applies:

| Order | Mechanism | Use for |
| --- | --- | --- |
| 1 | **UI flow in `Given`/`Background`** | **Default.** Build the prerequisite by clicking through the real user flow |
| 2 | **SQL seed + tag-scoped `@Before` hook** | **Exception only** — prerequisite feature is blocked/unimplemented or un-createable through the app |
| 3 | **API driver** | Verifying backend state independently of the UI, or last-resort setup when no UI path exists yet |

**Why UI-first:** driving the real flow exercises the create path, the navigation, and the
persistence — a regression anywhere fails the test. A seed skips all of that and only proves
rendering. When you must seed, record *why* (the blocking feature) so it can be removed later. Global
cleanup handles teardown — never write a per-slice cleanup file (see [world-and-hooks](references/world-and-hooks.md)).

## Designing for scale — never assume a clean database

Tests run against shared environments that already hold many records, are paginated, and may
lazy-load. A test written against a "fresh start" will flake or silently pass against the wrong row.

- **Identify by content, not position** — find your record with `.filter(setHasText(<TestWorld
  value>))` or `.getByText(...)`, not `.first()`/`.nth()`.
- **Search or filter before you assert** — narrow the list to your own data first.
- **Handle pagination / lazy-loading** — page or scroll to your record; don't assert absence just
  because page one doesn't show it.
- **Avoid whole-list counts** (`hasCount(n)`) against shared data — scope the count to your own data
  or assert the specific item.
- **Keep integrity** — these robustness measures must never become "assert something is visible so it
  passes." The assertion must still be the exact behaviour the Gherkin describes.

## References

- [four-layer-architecture](references/four-layer-architecture.md) — layer map with class-level anatomy
- [gherkin-and-dsl](references/gherkin-and-dsl.md) — DSL, tags, Backlog ID, scenario naming, step reuse
- [page-object-patterns](references/page-object-patterns.md) — locators, `assertThat`, response waits, no positional selection
- [world-and-hooks](references/world-and-hooks.md) — TestWorld state, hook lifecycle, scoped seeds
- [seeding-conventions](references/seeding-conventions.md) — when to seed, id ranges, FK-safe order
- [email-testing](references/email-testing.md) — `@requires-email`, Awaitility, what to assert
- [examples](references/examples.md) — one full end-to-end worked example
