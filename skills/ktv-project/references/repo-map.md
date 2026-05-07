# Repo Map

## Top Level

- `ktv-backend`: Spring Boot backend
- `admin-frontend`: admin console
- `room-frontend`: room-side ordering and playback app
- `sql`: initialization scripts
- `docs`: design and historical review notes

## Backend

Core backend package: `ktv-backend/src/main/java/com/ktv`

- `common`
  - `result`: unified response wrapper
  - `exception`: business and global exception handling
  - `annotation` + `aspect`: rate limiting
  - `i18n`: message helpers and keys
- `config`
  - `CorsConfig`
  - `RedisConfig`
  - `MyBatisPlusConfig`
  - `WebMvcConfig`
  - `SwaggerConfig`
- `controller/admin`
  - `AuthController`
  - `SongController`
  - `SingerController`
  - `CategoryController`
  - `RoomController`
  - `OrderController`
- `controller/room`
  - `SongSearchController`
  - `PlayQueueController`
  - `PlayControlController`
  - `HotSongController`
  - `RoomOrderController`
- `controller`
  - `MediaStreamController`
  - `HealthController`
- `service/impl`
  - `SongServiceImpl`
  - `SingerServiceImpl`
  - `CategoryServiceImpl`
  - `RoomServiceImpl`
  - `OrderServiceImpl`
  - `SongSearchServiceImpl`
  - `PlayQueueServiceImpl`
  - `PlayControlServiceImpl`
  - `HotSongServiceImpl`
  - `MediaServiceImpl`
- `mapper`
  - Java mapper interfaces
- `src/main/resources/mapper`
  - MyBatis XML definitions

## Admin Frontend

Core path: `admin-frontend/src`

- `api`: axios wrappers for admin modules
- `pages/Login`
- `pages/Song`
- `pages/Singer`
- `pages/Category`
- `pages/Room`
- `pages/Order`
- `layouts/AdminLayout.jsx`
- `router/index.jsx`
- `store/userStore.js`

## Room Frontend

Core path: `room-frontend/src`

- `api`
  - `request.js`
  - `song.js`
  - `queue.js`
  - `play.js`
- `pages/Join`
- `pages/Search`
- `pages/Queue`
- `components/PlayBar`
- `components/VideoPlayer`
- `layouts/MainLayout.jsx`
- `store/roomStore.js`

## Config and data

- backend config: `ktv-backend/src/main/resources/application.yml`
- i18n bundles: `ktv-backend/src/main/resources/i18n`
- schema: `sql/init-schema.sql`
- seed data: `sql/init-data.sql`

## Useful correlation patterns

- admin CRUD page name usually maps to matching backend controller/service/entity
- room-side queue and play-control bugs usually span React page state, Redis-backed service logic, and order-song persistence
- media problems often involve both file path validation and HTTP range streaming
