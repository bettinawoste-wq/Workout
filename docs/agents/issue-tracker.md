# Issue tracker

**Type:** GitHub Issues
**Repo:** bettinawoste-wq/Workout
**CLI:** `gh` (GitHub CLI), installed and authenticated as `bettinawoste-wq`

## Conventions

- Create issues with `gh issue create --repo bettinawoste-wq/Workout --title "..." --body "..."`.
- List open issues with `gh issue list --repo bettinawoste-wq/Workout`.
- Close issues with `gh issue close <number> --repo bettinawoste-wq/Workout --comment "..."`.
- Child-issue / blocking relationships: use GitHub's native "Development" sub-issue and "Blocked by" linking where available; otherwise note the relationship in the issue body.

## Pull requests

- Changes are built with the `implement` skill: test-first, then `code-review` against the judgement rules in `CODING_STANDARDS.md`.
- Work that has no issue yet, coming straight from a grilling, gets one before it is implemented. Write it from the agreed summary, with the decisions as acceptance criteria.
- One pull request per issue, always against `master`, with "Fixes #<n>" in the body.
- An issue that waits for another starts once that issue's pull request is merged, from the updated `master`. Building on the other pull request's branch instead lands its merge in that branch, not in `master`.
- `implement-spec` follows the same rules: each issue's own pull request branch is its integration branch, a dependent issue starts after its blocker's pull request merges, and there is no combined pull request.

## PRs as a request surface

Off. Pull requests are not treated as an incoming request queue for triage.
