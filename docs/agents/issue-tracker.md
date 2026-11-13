# Issue tracker

**Type:** GitHub Issues
**Repo:** bettinawoste-wq/Workout
**CLI:** `gh` (GitHub CLI), installed and authenticated as `bettinawoste-wq`

## Conventions

- Create issues with `gh issue create --repo bettinawoste-wq/Workout --title "..." --body "..."`.
- List open issues with `gh issue list --repo bettinawoste-wq/Workout`.
- Close issues with `gh issue close <number> --repo bettinawoste-wq/Workout --comment "..."`.
- Child-issue / blocking relationships: use GitHub's native "Development" sub-issue and "Blocked by" linking where available; otherwise note the relationship in the issue body.

## Landing changes

No pull requests: changes land on `master` directly, so its history stays linear, with no merge commits.

- Changes are built with the `implement` skill: test-first, then `code-review` against the judgement rules in `CODING_STANDARDS.md`.
- Work that has no issue yet, coming straight from a grilling, gets one before it is implemented. Write it from the agreed summary, with the decisions as acceptance criteria.
- Each issue is built on a local branch off `master`, and its commit message carries "Fixes #<n>", so the push closes the issue. Review-fix commits are squashed into the commit they fix.
- CI runs only after the push, so the branch must pass CI's checks locally first: `python scripts/check-text-files.py`, `./gradlew assembleDebug lintDebug testDebugUnitTest`, and the instrumented tests via `python scripts/device-test.py` on the attached phone. If no phone is attached, say so instead of skipping silently.
- Nothing is pushed until the owner has reviewed the local diff and says so. Then rebase the branch onto `master`, fast-forward `master`, push `master`, and delete the branch.
- An issue that waits for another starts once that issue has landed on `master`.
- `implement-spec` follows the same rules: each issue gets its own local branch, a dependent issue starts after its blocker has landed, and nothing lands as a combined change.

## PRs as a request surface

Off. Pull requests are not treated as an incoming request queue for triage.
