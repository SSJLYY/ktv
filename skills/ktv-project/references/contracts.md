# Contracts

## Admin CRUD contract tracing

Check in this order:

1. `admin-frontend/src/pages/<Module>/index.jsx`
2. `admin-frontend/src/api/<module>.js`
3. `ktv-backend/src/main/java/com/ktv/controller/admin/*Controller.java`
4. matching service interface and implementation
5. mapper interface and XML
6. entity, DTO, VO

High-risk drift:

- frontend form field names differ from DTO field names
- list response shape differs from table column expectations
- enum numeric values differ from page label mapping
- pagination wrapper fields differ from UI table consumption

## Room-side queue and play contract tracing

Check in this order:

1. `room-frontend/src/pages/Join`
2. `room-frontend/src/pages/Search`
3. `room-frontend/src/pages/Queue`
4. `room-frontend/src/api/song.js`, `queue.js`, `play.js`, `request.js`
5. `controller/room/*`
6. `PlayQueueServiceImpl`, `PlayControlServiceImpl`, `SongSearchServiceImpl`, `RoomServiceImpl`
7. `OrderMapper`, `OrderSongMapper`, related XML
8. Redis key usage

High-risk drift:

- room join state and actual room availability mismatch
- current play item shape differs from frontend expectations
- queue item status label differs from backend status enum
- top-song or cut-song modifies order but not current play state

## Media contract tracing

Check in this order:

1. frontend media URL construction
2. `MediaStreamController`
3. `MediaServiceImpl`
4. `FileSecurityChecker`
5. path fields in song entity and mapper XML
6. runtime `MEDIA_BASE_PATH`

High-risk drift:

- stored path is relative while backend assumes absolute
- file exists but security rules reject it
- frontend expects video while record points to audio only
- range and content-type behavior differ by file extension
