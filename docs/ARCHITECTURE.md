# DeepInsight Architecture

This repo is split into a Vue frontend and a Spring Boot backend. Keep the structure boring and direct: one concept should have one obvious home.

## Frontend Map

- `src/api/`: HTTP client and typed endpoint modules. New API calls go here.
- `src/stores/`: Pinia stores for app-wide state.
- `src/composables/`: reusable Vue composition functions.
- `src/components/`: shared UI components grouped by purpose.
- `src/views/`: route-level pages.
- `src/utils/`: pure helpers with no UI ownership.
- `src/styles/` and `src/index.css`: global design tokens and Element Plus overrides.
- `src/types/`: shared TypeScript data contracts.

Avoid adding new top-level folders that duplicate these jobs. In particular, do not recreate `src/services/` for HTTP wrappers or `src/store/` for Pinia aliases.

## Backend Map

- `controller/`: REST entrypoints.
- `service/`: business workflows.
- `repository/`: persistence access.
- `entity/`: JPA entities and database shape.
- `dto/`: request/response contracts.
- `security/`: authentication and authorization.
- `config/`: framework configuration.
- `common/`: shared result/error helpers.

The backend is currently organized by technical layer. If it grows further, move one domain at a time into feature packages such as `training`, `knowledge`, `forum`, `ai`, and `admin`, keeping controller/service/repository/entity files together inside that feature. Do not do a whole-backend package move without a passing backend test/build baseline.

Official model seed data and generated model article content live in `ModelCatalogService`; `ModelController` should stay focused on HTTP request handling and ownership checks.

## API Auth Boundary

- Auth, forum browsing, uploads, model articles, and official model reads are public.
- `GET /api/v1/models` and `GET /api/v1/models/official` are public read endpoints; create, update, delete, and seed operations remain authenticated.
- Frontend training model selection depends on the public official model read path, while user-specific models appear only when a valid principal is present.

## Database Safety

The database contains user data and generated content. Any schema rebuild must follow this sequence:

1. Export a timestamped backup with `npm run db:backup`.
2. Create a safe copy with `npm run db:rebuild-copy`.
3. Create or update migration SQL in `backend/src/main/resources/db/`.
4. Apply migration to the copied database first.
5. Compare table counts before and after.
6. Only then apply to the live `deepinsight` database.

Do not drop tables in migration scripts unless the data has been exported and the migration includes a verified restore path.

Runtime schema changes are disabled by default. The backend uses `JPA_DDL_AUTO=validate` unless a developer explicitly overrides it, so entity changes must be captured in `backend/src/main/resources/db/` migration files instead of relying on Hibernate `update`.

## Startup Safety

- Backend dev startup uses `scripts/start-backend.ps1`, the repo-local `backend/mvnw.cmd`, and JDK 21 by default.
- Override Java with `DEEPINSIGHT_JAVA_HOME` only when that path points at a compatible JDK.
- Frontend dev startup uses `scripts/start-frontend.ps1`; production/deploy static serving uses `deploy/serve-dist.cjs` to avoid `npx` downloads at startup.

## Current Cleanup Decisions

- API access lives in `src/api/`; shallow service re-export files were removed.
- Pinia stores live in `src/stores/`; shallow `src/store/` aliases were removed.
- Visualization run selection lives in `src/composables/useRunSelector.ts`.
- Markdown rendering lives in `src/utils/markdown.ts`.
- Redis repositories are disabled because Redis is used through `RedisTemplate`, while JPA repositories own SQL persistence.
