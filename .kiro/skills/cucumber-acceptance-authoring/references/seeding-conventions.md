# Seeding conventions

## Seeding is the exception, not the default

Before writing any seed SQL, ask: **can the scenario build this prerequisite by driving the app's real
UI flow?** If yes, do that instead — a `Given`/`Background` step that creates the data through the UI.
A seed only proves that an inserted row renders; driving the flow exercises the create path,
navigation, and persistence, so a regression anywhere fails the test.

**Write a seed file only when the prerequisite is blocked:**
- the feature that would create the state is **not implemented yet**,
- the state **cannot be produced through the app** at all, or
- seeding is genuinely the **only viable way** (e.g. a backend-only historical status).

When you seed for one of these reasons, record *why* in a comment in the seed file (or the hook) so it
can be deleted once the blocking feature ships. Everything below applies only to these blocked cases.

## Directory layout

```
resources/fixtures/sql/
├── widget/               ← WIDG-* seeds
├── <domain>/             ← per-domain seeds
├── global-cleanup.sql    ← deletes all rows above the high watermark, FK-safe order
└── global-sequence-init.sql  ← resets sequences into the test id range
```

Place new fixtures in the domain subdirectory matching the story prefix.

## ID ranges — a high watermark keeps test data separable

All fixture IDs use values above the watermark the global cleanup deletes (this example uses
`900000`, with sequences reset to `990001`). Give each epic its own sub-range so fixtures never
collide:

| Epic | Reserved range | Example IDs |
| --- | --- | --- |
| First epic | 990 001–990 999 | `990001`, `990002` |
| Second epic | 991 000–991 999 | `991001`, `991002` |
| Shared reference data | 992 000–992 999 | `992001` |
| New epics | next free block | continue the pattern |

Pick the watermark comfortably above any real ID the app will ever generate, and keep the same
watermark in `global-cleanup.sql`, `global-sequence-init.sql`, and every seed.

## Naming

| Pattern | When |
| --- | --- |
| `<EPIC>-<N>-seed.sql` | One seed covers all slices of a story |
| `<EPIC>-<N>-<variant>-seed.sql` | Story needs multiple variants (e.g. `-edit-seed.sql`) |
| `<EPIC>-<N>.<S>-seed.sql` | A slice has unique data needs |

Name by the Backlog ID, not any external ticket key.

## INSERT conventions

- Explicit column lists — never rely on column order.
- Stamp an audit column (e.g. `created_by = 'acceptance-tests'`) so test rows are identifiable in logs.
- Name test entities recognisably: `ACCEPTANCE_TEST <EPIC-N> <DESCRIPTION>`
  (e.g. `'ACCEPTANCE_TEST WIDG-3 WIDGET'`).
- Insert FK dependencies before the rows that reference them (parents before children).
- Make seeds idempotent where the DB supports it (`ON CONFLICT DO UPDATE` / `MERGE`) so a re-run is safe.

## FK-safe delete order

`global-cleanup.sql` deletes leaf/child tables first, parents last. A story-scoped teardown (rarely
needed — global cleanup usually suffices) follows the same reverse-dependency order for the rows it
owns. Never delete a parent before the children that reference it.

## Shared fixtures

If a shared reference fixture exists (data many stories depend on), never modify it for a single
story's needs. Create a story-scoped seed instead.
