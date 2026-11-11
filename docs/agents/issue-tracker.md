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

- Changes are built with the `implement` skill (test-first, then `code-review`), one pull request per issue.
- One pull request per issue, always against `master`, with "Fixes #<n>" in the body.
- An issue that waits for another starts once that issue's pull request is merged, from the updated `master`. Building on the other pull request's branch instead lands its merge in that branch, not in `master`.

## PRs as a request surface

Off. Pull requests are not treated as an incoming request queue for triage.
