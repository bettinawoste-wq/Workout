# Domain docs

**Layout:** single-context

This repo is a single Android application module (no monorepo/workspace signals found), so domain knowledge lives in one place at the repo root:

- `CONTEXT.md` — living description of the domain/architecture.
- `docs/adr/` — Architecture Decision Records, one file per significant decision.

## Consumer rules

- Skills and agents should read `CONTEXT.md` (if present) before making architectural changes, and treat it as the source of truth over inferring conventions from code alone.
- A new architectural decision (e.g. choosing a DI framework, changing the persistence layer, restructuring modules) should be recorded as a new file under `docs/adr/`, not just left implicit in a commit message.
- If this repo grows into multiple independently-versioned modules later, revisit this file and switch to the multi-context layout (`CONTEXT-MAP.md` + per-context `CONTEXT.md`).
