---
name: ktv-project
description: Use this skill when working in the KTV repo for project-wide code reading, codereview, bug fixing, API tracing, contract debugging, SQL or Redis investigation, media playback issues, queue and order state problems, or project-specific validation across ktv-backend, admin-frontend, room-frontend, sql, and docs.
---

# KTV Project

## When to use

Use this skill for tasks in this repository when the user asks for project reading, codereview, bug fixing, continued review passes, frontend-backend debugging, API tracing, playback debugging, queue debugging, order or room troubleshooting, or repo-local documentation and skill updates.

This skill is for repo-specific work. Do not answer from generic Spring Boot, React, MyBatis, Redis, or media-streaming assumptions when the code can be checked locally.

## Core rules

- Trust code over old prose docs. Several legacy files under `skills/*.md`, `docs/*.md`, and root markdown files contain mojibake or stale statements.
- Treat project encoding as UTF-8. If Chinese text looks corrupted, verify file encoding before trusting or reusing the text.
- For codereview tasks, prioritize real defects, regressions, contract drift, null handling, status transitions, and persistence correctness over style-only cleanup.
- When the user asks to continue review passes, keep scanning for the next concrete issue instead of stopping after the first passing build.
- For cross-layer bugs, trace the complete path:
  - frontend page or component
  - frontend api wrapper
  - backend controller
  - service
  - mapper and xml
  - SQL schema or seed data
  - Redis state if queue or hot-song behavior is involved

## Repo map

- `ktv-backend`
  - Spring Boot backend
  - business truth for most workflows
- `admin-frontend`
  - admin console, React + Vite + Ant Design
- `room-frontend`
  - room-side ordering and playback UI, React + Vite + Ant Design Mobile
- `sql`
  - schema and seed data
- `docs`
  - secondary reference only; verify against source
- `skills`
  - this repo-local skill set

Read `references/repo-map.md` when you need exact module landmarks.

## Runtime facts to confirm early

- backend port: `8080`
- admin frontend dev port: `3000`
- room frontend dev port: `3001`
- backend database in current config: `ktv_db`
- media root defaults to `MEDIA_BASE_PATH` or `/data/ktv-media`
- CORS defaults allow `http://localhost:3000,http://localhost:3001`
- backend response wrapper is under `com.ktv.common.result`
- mapper XML lives in `ktv-backend/src/main/resources/mapper`

## High-yield debugging areas

### 1. Admin-side CRUD and management

Typical files:

- `admin-frontend/src/pages/*`
- `admin-frontend/src/api/*.js`
- `ktv-backend/src/main/java/com/ktv/controller/admin/*`
- matching service and mapper xml

Common defects:

- table field and VO field mismatch
- form submit payload missing required id or status
- request wrapper hiding backend business message
- pagination parameter drift
- soft-delete and status filters not aligned

### 2. Room join, queue, and play-control

Typical files:

- `room-frontend/src/pages/Join`
- `room-frontend/src/pages/Search`
- `room-frontend/src/pages/Queue`
- `room-frontend/src/components/PlayBar`
- `room-frontend/src/components/VideoPlayer`
- `room-frontend/src/api/*.js`
- `ktv-backend/src/main/java/com/ktv/controller/room/*`
- `PlayQueueServiceImpl`
- `PlayControlServiceImpl`
- `SongSearchServiceImpl`
- `RoomServiceImpl`

Common defects:

- queue status not synced to DB and Redis consistently
- current playing item and queue list diverge
- repeat, cut-song, top-song, delete-song state transitions overwrite each other
- room join accepts invalid room state
- frontend polling and backend state model disagree

### 3. Media playback and streaming

Typical files:

- `MediaStreamController`
- `MediaServiceImpl`
- `MediaUtils`
- `FileSecurityChecker`
- song media path fields in entity, mapper, and SQL data
- `room-frontend/src/components/VideoPlayer`
- `room-frontend/src/components/PlayBar`

Common defects:

- HTTP range handling incomplete
- local path blocked by security checker
- record path exists in DB but actual file root config is wrong
- audio and video branches use different assumptions
- MIME type or extension fallback is wrong

### 4. Auth, permission, and token flow

Typical files:

- `admin-frontend/src/api/request.js`
- `admin-frontend/src/store/userStore.js`
- `AuthController`
- `JwtInterceptor`
- `JwtUtil`
- `application.yml`

Common defects:

- token not injected or cleared correctly
- backend returns business error but frontend converts it to generic error
- route guard and backend auth state drift
- CORS and preflight behavior differ between admin and room apps

## Validation strategy

Pick the smallest validation that proves the change, then broaden only if needed.

- backend:
  - `cd ktv-backend`
  - `mvn test`
  - `mvn clean package -DskipTests`
- admin frontend:
  - `cd admin-frontend`
  - `npm run build`
- room frontend:
  - `cd room-frontend`
  - `npm run build`
- single-file JS syntax gate:
  - `node --check <file>`

If the fix crosses layers, validate both touched layers. If the user asked for broad codereview continuation, keep scanning after the first successful validation.

Read `references/commands.md` for concrete command sets.

## Investigation workflow

### Codereview pass

1. Scan touched modules and current repo state.
2. Start with business-critical transitions:
   - room status
   - order lifecycle
   - queue lifecycle
   - playback lifecycle
   - media stream path
3. Reproduce or validate with the narrowest command set.
4. Fix directly.
5. Re-run targeted validation.
6. Continue to the next likely issue if the user wants a full sweep.

### Contract tracing

1. Read frontend page or component.
2. Read matching frontend `api/*.js`.
3. Confirm request path, params, and expected response shape.
4. Read backend controller and service.
5. Read mapper XML when list queries, joins, or derived fields are involved.
6. Check schema and seed data if statuses, defaults, or enum labels affect behavior.

Read `references/workflows.md` and `references/contracts.md` when you need detailed guidance.

## Project-specific patterns

- Backend codebase is the main business truth.
- Mapper XML is not optional context; many data-shape issues live there.
- Redis is used for queue state, hot songs, and playback coordination.
- `application.yml` contains meaningful runtime defaults; verify them before assuming infra drift.
- Old docs often describe versions that do not match the current checkout. Confirm from `package.json` and `pom.xml`.

## Documentation and skill updates

When updating repo-local docs or skills:

- prefer short, code-backed instructions
- do not copy mojibake text forward
- name real modules and files exactly as they exist
- include only commands that match this checkout
- keep reference files targeted and loadable on demand

## Additional references

- `references/repo-map.md`
- `references/workflows.md`
- `references/contracts.md`
- `references/commands.md`
- `references/review-checklist.md`
