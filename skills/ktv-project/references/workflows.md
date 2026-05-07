# Workflows

## 1. Codereview pass

Use this flow when the user asks for `codereview` mode, full-project bug hunting, or repeated continuation:

1. Scan current repo state and touched modules.
2. Identify high-risk code paths first:
   - queue and playback state transitions
   - room open/close and order lifecycle
   - admin CRUD request validation
   - media streaming and file access
   - auth, token, and permission checks
3. Reproduce with the narrowest useful validation.
4. Fix defects directly.
5. Re-run targeted validation, then broaden if the fix crosses layers.
6. Continue scanning instead of stopping at the first green build when the user says `继续`.

## 2. Frontend-backend contract tracing

Use this when a page renders wrong fields, a button has no effect, or a business error disappears:

1. Read the frontend page under `pages/`.
2. Read its corresponding `src/api/*.js` wrapper.
3. Confirm request URL, params, and response shape.
4. Read the backend controller and service.
5. Check mapper XML if pagination, joins, or derived fields are involved.
6. Confirm schema or seed data when behavior depends on enum/status/default values.

Common risk points:

- frontend wrapper swallowing backend business payloads
- DTO/VO field drift
- mapper XML alias mismatch
- camelCase and underscore mapping assumptions

## 3. Media playback debugging

1. Confirm the frontend uses the expected media URL.
2. Check `MediaStreamController` for range handling.
3. Check `MediaServiceImpl` and `MediaUtils` for path resolution.
4. Check `FileSecurityChecker` for false-positive blocking.
5. Confirm the song record contains a valid local media path or derived path.
6. If playback differs between audio and video, inspect `VideoPlayer` and `PlayBar` separately.

## 4. Validation choices

Use the smallest credible command set:

- backend only:
  - `mvn test`
  - or `mvn clean package -DskipTests`
- admin frontend only:
  - `npm run build`
- room frontend only:
  - `npm run build`
- syntax-only JS sanity if needed:
  - `node --check <file>`

## 5. Documentation updates

When updating docs or skills for this repo:

- prefer concise, correct, code-backed instructions
- do not copy stale claims from old markdown blindly
- document module names and validation commands exactly as they exist in the checkout
