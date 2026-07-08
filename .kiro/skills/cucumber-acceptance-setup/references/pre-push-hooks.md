# Git pre-push hook

Wiring a pre-push hook keeps broken acceptance tests off the shared branch. But acceptance tests are
**slow and environment-dependent** — they need the app running, a reachable database, and Playwright
browsers installed. Blocking every push on the full suite trains people to `git push --no-verify`,
which defeats the point.

## What to actually run on pre-push

Tier the checks by cost, and make the expensive tier conditional:

1. **Always (fast, ~seconds):** compile the acceptance module — `cd {{MODULE_PATH}} && mvn test-compile`.
   This catches broken step defs, page objects, and glue wiring without launching a browser. This
   alone prevents most "green locally, red in CI" pushes.
2. **When a stack is reachable (opt-in):** run a `@smoke` or `@pre-push` tagged subset against the
   running app. Skip cleanly with a message if the app/DB is not up — never fail a push because the
   developer doesn't happen to have the stack running.
3. **Never on pre-push:** the full suite. That belongs in CI, where the stack is provisioned
   deterministically.

Tag a handful of fast, high-value scenarios `@pre-push` (or reuse `@smoke`) so tier 2 stays quick.

## Choosing a hook mechanism

| Mechanism | Use when | Versioned? |
| --- | --- | --- |
| **Husky** | The repo already has `package.json` / Node tooling (typical for an Angular or Nx repo) | Yes — committed under `.husky/` |
| **Native `.git/hooks/pre-push`** | No Node tooling, or you want zero dependencies | No — per-clone, must be installed by each dev |
| **Nx target + Husky** | Nx monorepo; you want `nx affected` to decide whether to run at all | Yes |

Prefer Husky in an Angular/Nx repo because the hook is committed and every clone gets it. Fall back to
the native hook for a Node-free Java repo.

## Option A — Husky (Angular / Nx repos)

Husky manages git hooks from committed files. If the repo doesn't already use it:

```bash
npm install --save-dev husky
npx husky init          # creates .husky/ and sets core.hooksPath
```

Then create `.husky/pre-push`:

```sh
#!/usr/bin/env sh

echo "[pre-push] compiling acceptance-test module…"
( cd {{MODULE_PATH}} && mvn -q test-compile ) || {
  echo "[pre-push] acceptance module failed to compile — push aborted."
  exit 1
}

# Only run smoke scenarios if the app is up; skip cleanly otherwise.
APP_URL="${APP_BASE_URL:-{{APP_BASE_URL}}}"
if curl -fsS --max-time 2 "$APP_URL" >/dev/null 2>&1; then
  echo "[pre-push] app reachable at $APP_URL — running @pre-push smoke scenarios…"
  ( cd {{MODULE_PATH}} && PLAYWRIGHT_HEADLESS=true \
    mvn -q test -Dcucumber.filter.tags="@pre-push" ) || {
    echo "[pre-push] smoke scenarios failed — push aborted."
    exit 1
  }
else
  echo "[pre-push] app not reachable at $APP_URL — skipping smoke run (compile passed). CI will run the full suite."
fi
```

Make it executable (`chmod +x .husky/pre-push`) and commit it. In an Nx repo you can replace the raw
`mvn` calls with `nx run acceptance-tests:build` and `nx test acceptance-tests --tag="@pre-push"` so
the hook goes through the same targets as everything else.

## Option B — Native git hook (no Node)

Create `.git/hooks/pre-push` (not versioned — document it in the README so each dev installs it, or
ship an install script):

```sh
#!/usr/bin/env sh
# Acceptance-test pre-push guard. Install: cp this file to .git/hooks/pre-push && chmod +x it.

echo "[pre-push] compiling acceptance-test module…"
( cd {{MODULE_PATH}} && mvn -q test-compile ) || exit 1

APP_URL="${APP_BASE_URL:-{{APP_BASE_URL}}}"
if curl -fsS --max-time 2 "$APP_URL" >/dev/null 2>&1; then
  ( cd {{MODULE_PATH}} && PLAYWRIGHT_HEADLESS=true mvn -q test -Dcucumber.filter.tags="@pre-push" ) || exit 1
else
  echo "[pre-push] app not up — skipping smoke run; CI covers the full suite."
fi
```

To make a native hook installable across the team without relying on `.git/hooks`, commit the script
to `hooks/pre-push` in the repo and point git at it once per clone:

```bash
git config core.hooksPath hooks
```

Now the committed `hooks/pre-push` is the active hook for everyone who runs that one command (add it
to the project's setup script).

## Calling Make targets from the hook (no Nx needed)

If the project uses Make (the common no-Nx case), point the hook at the Make targets so there is one
source of truth for how the harness runs. Works from either a Husky or a native hook body:

```sh
#!/usr/bin/env sh

echo "[pre-push] compiling acceptance-test module…"
make accept-compile || {
  echo "[pre-push] acceptance module failed to compile — push aborted."
  exit 1
}

APP_URL="${APP_BASE_URL:-{{APP_BASE_URL}}}"
if curl -fsS --max-time 2 "$APP_URL" >/dev/null 2>&1; then
  echo "[pre-push] app reachable — running smoke scenarios…"
  make accept-smoke || { echo "[pre-push] smoke scenarios failed — push aborted."; exit 1; }
else
  echo "[pre-push] app not up — skipping smoke run; CI covers the full suite."
fi
```

`accept-compile` and `accept-smoke` are the targets defined in
[build-tool-integration](build-tool-integration.md). This keeps the hook trivial and guarantees it
runs exactly what `make accept-*` runs. A repo with no Node tooling can install this via the native
`core.hooksPath` route (Option B) and never touch Husky or Nx.

## Option C — Nx `affected` gate

In a busy monorepo, only run the acceptance checks when something that could affect them changed:

```sh
#!/usr/bin/env sh
if npx nx show projects --affected --type app 2>/dev/null | grep -q acceptance-tests; then
  nx run acceptance-tests:build || exit 1
else
  echo "[pre-push] acceptance-tests not affected — skipping."
fi
```

## Rules that keep the hook from being hated

- **Fast by default.** Tier 1 (compile) must finish in seconds. If the smoke tier creeps past ~30s,
  trim the `@pre-push` set.
- **Degrade, don't block.** No running stack → skip the browser tier with a clear message, don't fail.
- **Idempotent + quiet.** Use `mvn -q`; print one clear line per phase so a developer knows what ran.
- **Escapable.** `git push --no-verify` bypasses it. That's fine for genuine emergencies — the full
  suite in CI is the real gate.
- **CI owns the full suite.** The hook is a fast smoke screen, not a replacement for CI running
  `-Dcucumber.filter.tags="not @wip"` against a provisioned stack.
