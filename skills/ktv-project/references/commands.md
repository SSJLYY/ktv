# Commands

## Backend

Working directory:

- `ktv-backend`

Common commands:

- `mvn test`
- `mvn clean package -DskipTests`
- `mvn spring-boot:run`

Use when:

- validating service, controller, mapper, or config changes
- confirming compile health after codereview fixes

## Admin frontend

Working directory:

- `admin-frontend`

Common commands:

- `npm run build`
- `npm run dev`
- `npm run lint`

Use when:

- validating admin page, route, table, form, or request-wrapper changes

## Room frontend

Working directory:

- `room-frontend`

Common commands:

- `npm run build`
- `npm run dev`
- `npm run lint`

Use when:

- validating join, search, queue, play bar, or video player changes

## Single-file checks

- `node --check <file>`

Use when:

- a full frontend build is too expensive for a narrow JS syntax sanity pass

## Git checks

- `git status --short`
- `git diff -- <path>`

Use when:

- confirming only intended files changed
- reviewing local deltas before closeout
