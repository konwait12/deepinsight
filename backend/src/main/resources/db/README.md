# Database Migration Rules

The live database must not be rebuilt in place without a backup.

Use this flow:

1. From the repo root, run `npm run db:backup`.
2. Run `npm run db:rebuild-copy` to create a full copied database.
3. Put schema changes in a new SQL migration file in this folder.
4. Make migrations idempotent where possible by checking `information_schema`.
5. Preserve data with `ALTER TABLE`, `CREATE TABLE ... SELECT`, or explicit backfill steps.
6. Verify row counts after the migration.

Existing SQL files are historical setup/migration scripts. New rebuild work should create a new dated migration instead of editing old migration history.
