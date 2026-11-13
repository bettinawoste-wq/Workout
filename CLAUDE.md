# CLAUDE.md

## Agent skills

### Issues and pull requests

Issues are tracked in GitHub Issues for [bettinawoste-wq/Workout](https://github.com/bettinawoste-wq/Workout), via the `gh` CLI. See `docs/agents/issue-tracker.md` for issues and pull requests.

### Domain docs

Single-context layout at the repo root: `GLOSSARY.md` (domain terms), `CONTEXT.md` (architecture) and `docs/adr/`. See `docs/agents/domain.md`.

### Device

Device tests and README screenshots on the attached phone: `scripts/` (`device-test.py`, `readme-screenshots.py`).

### Text-file check

Run `python scripts/check-text-files.py` (UTF-8, line width 100) before pushing; CI checks the same.
