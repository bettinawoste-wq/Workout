# Issue tracker

**Type:** GitHub Issues
**Repo:** bettinawoste-wq/Workout
**CLI:** `gh` (GitHub CLI)

## Setup required

The `gh` CLI is not currently installed on this machine. Install it before relying on skills that read/write issues (e.g. `to-tickets`, `triage`, `to-spec`, `wayfinder`):

- Windows: `winget install --id GitHub.cli` or `scoop install gh`
- After install, run `gh auth login` once to authenticate against github.com.

## Conventions

- Create issues with `gh issue create --repo bettinawoste-wq/Workout --title "..." --body "..."`.
- List open issues with `gh issue list --repo bettinawoste-wq/Workout`.
- Close issues with `gh issue close <number> --repo bettinawoste-wq/Workout --comment "..."`.
- Child-issue / blocking relationships: use GitHub's native "Development" sub-issue and "Blocked by" linking where available; otherwise note the relationship in the issue body.

## PRs as a request surface

Off. Pull requests are not treated as an incoming request queue for triage.
