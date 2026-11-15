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

Every change lands on `master` through a pull request, merged with a merge commit.

- Changes are built with the `implement` skill: test-first, then `code-review` against the judgement rules in `CODING_STANDARDS.md`.
- Work that has no issue yet, coming straight from a grilling, gets one before it is implemented. Write it from the agreed summary, with the decisions as acceptance criteria.
- Each issue is built on its own branch off `master`. Before the push, clean up its commits: squash review-fix and fixup commits into the commit they fix, and rebase onto the current `master`, so each commit stands on its own.
- Run CI's checks locally before the push: `python scripts/check-text-files.py`, `./gradlew assembleDebug lintDebug testDebugUnitTest`, and the instrumented tests via `python scripts/device-test.py` on the attached phone. If no phone is attached, say so instead of skipping silently.
- After `implement`, push the branch and open the pull request without waiting; its body carries "Fixes #<n>", so the merge closes the issue.
- Merge only after CI is green and the owner says so: `gh pr merge <n> --merge --delete-branch`. Then update the local `master`.
- An issue that waits for another starts once that issue's pull request is merged.
- `implement-spec` follows the same rules: each issue gets its own branch and pull request, a dependent issue starts after its blocker is merged, and nothing lands as a combined change.

## PRs as a request surface

Off. Pull requests are not treated as an incoming request queue for triage.
