# 4. Exported Room schemas for migration tests

## Status

Accepted

## Context

Every release so far (`v1.0.0`, `v1.0.1`, `v1.1`) shipped database version 4, so real phones hold version 4 data that each future migration must carry forward. With `exportSchema = false`, a migration test had to rebuild the old tables by hand in SQL, which can quietly drift from what a release really created. Room's `MigrationTestHelper` can create a database at any old version and validate a migration's result, but only from that version's exported schema.

## Decision

Export the schema. The Room Gradle plugin writes `app/schemas/com.wob.workout.data.database.AppDatabase/<version>.json` on every build; the files are committed and packed into the test APK as assets. `4.json` was generated after the fact by building tag `v1.1` with export switched on; `5.json` comes from the current entities.

Migration tests use Room's `MigrationTestHelper`. `AllMigrationsTest` migrates version 4 to the current version with `AppDatabase.ALL_MIGRATIONS`, the same list `DatabaseProvider` registers.

Migrations 2→3 and 3→4 stay in the code but untested: versions 2 and 3 never shipped in a release, only on development installs, and their schemas were never exported.

## Considered Options

Keeping the hand-built version 4 database in `Migration4To5Test` would avoid the generated files in the repo. It was rejected because every new migration would repeat the hand-written SQL, and nothing would check it against what a release actually created.

## Consequences

- Every bump of `AppDatabase`'s `version` commits the new `<version>.json`, adds its `Migration` to `ALL_MIGRATIONS`, and adds a migration test built on Room's `MigrationTestHelper`.
- A committed schema file must never be edited by hand or regenerated for an old version; it records what that version shipped.
- The project's own ViewModel test scaffold, formerly also called `MigrationTestHelper`, is now `TestAppContainer`.
