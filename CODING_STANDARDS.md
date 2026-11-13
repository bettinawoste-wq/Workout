# Coding standards

Judgement rules for review. Mechanical rules (encoding, line width) belong in `scripts/check-text-files.py`, where CI enforces them, not here.

- **SQL string splits:** split a long SQL string like the existing DAOs do: the space leads the continuation fragment, as in `+ " WHERE sessionId = :sessionId"`.
- **One name per class:** within a file, a class keeps one name: either the imported short name everywhere or the qualified name everywhere.
