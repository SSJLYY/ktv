# KTV Karaoke System

> Modern KTV management system based on Java 21 + Spring Boot 3 + React 18

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-green)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue)](https://reactjs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Introduction

KTV Karaoke System is a complete KTV venue management solution with two main modules: admin management system and room karaoke system. The system adopts a front-end and back-end separation architecture, supporting core business features such as song selection, queue management, playback control, and room management.

### Core Features

- **Admin Management**: Song management, singer management, category management, room management, order management, user permissions
- **Room Karaoke**: Multi-dimensional song search (pinyin/singer/category/hot), song queuing, playback control, history records
- **Media Playback**: Support for MP3/FLAC audio and MP4 video files with actual playback, streaming supports progress dragging
- **Redis Optimization**: Hot rankings, song caching, queue management, playback state sync

## Tech Stack

### Backend

| Technology | Version | Description |
|------------|---------|-------------|
| Java | 21 LTS | Virtual Threads support |
| Spring Boot | 3.2.x | Jakarta EE 10 |
| MyBatis-Plus | 3.5.7 | Simplified ORM |
| MySQL | 8.0 | Primary data storage |
| Redis | 6.x+ | Cache/Queue/Hot rankings |
| JWT | Stateless auth | Front-end/back-end separation |

### Frontend

| Technology | Version | Description |
|------------|---------|-------------|
| React | 18 | Functional components + Hooks |
| Vite | 5.x | Build tool |
| Ant Design | 5.x | Admin UI |
| Ant Design Mobile | 5.x | Room UI |
| Zustand | 4.x | Lightweight state management |
| Axios | 1.x | HTTP client |

## Quick Start

### Prerequisites

- JDK 21+
- Node.js 18+
- MySQL 8.0+
- Redis 6.x+
- Maven 3.6.3+

### Database Initialization

```bash
# Create database
CREATE DATABASE ktv CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# Import schema
mysql -u root -p ktv < sql/init-schema.sql

# Import initial data
mysql -u root -p ktv < sql/init-data.sql
```

### Backend Startup

```bash
cd ktv-backend

# Modify configuration file
# src/main/resources/application.yml
# Configure database connection, Redis connection, media file path

# Start project
mvn spring-boot:run
```

Backend default port: `http://localhost:8080`

### Frontend Startup

#### Admin Frontend

```bash
cd admin-frontend

# Install dependencies
npm install

# Start dev server
npm run dev
```

Access: `http://localhost:3000`
Default account: `admin` / `admin123`

#### Room Frontend

```bash
cd room-frontend

# Install dependencies
npm install

# Start dev server
npm run dev
```

Access: `http://localhost:3001`

## Project Structure

```
ktv/
├── ktv-backend/                    # Spring Boot backend
│   ├── src/main/java/com/ktv/
│   │   ├── config/                 # Configuration classes
│   │   │   ├── CorsConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   ├── MyBatisPlusConfig.java
│   │   │   └── WebMvcConfig.java
│   │   ├── controller/             # Controllers
│   │   │   ├── admin/              # Admin APIs /api/admin/**
│   │   │   └── room/               # Room APIs /api/room/**
│   │   ├── service/                # Business logic
│   │   │   └── impl/
│   │   ├── mapper/                 # MyBatis Mappers
│   │   ├── entity/                 # Entity classes
│   │   ├── dto/                    # Request DTOs
│   │   ├── vo/                     # Response VOs
│   │   ├── common/                 # Common components
│   │   │   ├── result/             # Unified response format
│   │   │   ├── exception/          # Global exception handler
│   │   │   └── enums/              # Enum classes
│   │   ├── interceptor/            # Interceptors
│   │   ├── task/                   # Scheduled tasks
│   │   └── util/                   # Utilities
│   └── src/main/resources/
│       └── application.yml
│
├── admin-frontend/                 # Admin frontend
│   ├── src/
│   │   ├── api/                    # API wrappers
│   │   ├── pages/                  # Page components
│   │   │   ├── Login/              # Login page
│   │   │   ├── Song/               # Song management
│   │   │   ├── Singer/             # Singer management
│   │   │   ├── Category/           # Category management
│   │   │   ├── Room/               # Room management
│   │   │   └── Order/              # Order management
│   │   ├── components/             # Shared components
│   │   ├── layouts/                # Layout components
│   │   ├── store/                  # Zustand state management
│   │   ├── router/                 # React Router v6
│   │   └── utils/                  # Utilities
│   └── vite.config.js
│
├── room-frontend/                  # Room karaoke frontend
│   ├── src/
│   │   ├── api/                    # API wrappers
│   │   ├── pages/                  # Page components
│   │   │   ├── Join/               # Join room
│   │   │   ├── Search/             # Song search
│   │   │   └── Queue/              # Song queue
│   │   ├── components/             # Shared components
│   │   │   ├── PlayBar/            # Bottom playback control bar
│   │   │   ├── SongCard/           # Song card
│   │   │   └── TabBar/             # Bottom navigation bar
│   │   ├── layouts/                # Layout components
│   │   ├── store/                  # Zustand state management
│   │   ├── router/                 # React Router v6
│   │   └── assets/                 # Static assets
│   └── vite.config.js
│
├── sql/                            # Database scripts
│   ├── init-schema.sql             # Schema
│   └── init-data.sql               # Initial data
│
├── docs/                           # Documentation
│   ├── README.md                   # Documentation index
│   ├── project-overview.md         # Project design doc
│   ├── api-reference.md            # API reference
│   ├── code-review-standards.md    # Code review standards
│   └── tasks/                      # Task documents
│
├── skills/                         # Development aids
│
├── README.md                       # Project description (Chinese)
├── README.en.md                    # Project description (English)
├── CONTRIBUTING.md                 # Contribution guide
└── LICENSE                         # MIT License
```

## Core Features

### Song Search

- **Pinyin Search**: Support pinyin initials and full pinyin search
- **Singer Search**: View songs by singer
- **Category Browse**: Browse by language, genre
- **Hot Rankings**: Real-time ranking based on play count (Redis ZSet)

### Song Queue

- Redis List for queue management
- Support top, remove, reorder
- Dual-write to database for consistency

### Playback Control

- **Play/Pause**: Control current song playback
- **Next Song**: Skip to next song
- **Replay**: Replay current song
- Support audio (MP3/FLAC) and video (MP4) files

### Media Files

- **Upload Management**: Admin backend supports uploading audio/video files
- **Streaming**: HTTP Range requests support progress dragging
- **Auto Detection**: Auto-select player based on file extension

### Redis Data Structures

| Key Format | Type | Description |
|------------|------|-------------|
| `ktv:queue:{orderId}` | List | Song queue |
| `ktv:playing:{orderId}` | String | Current playing song ID |
| `ktv:play:status:{orderId}` | String | Playback status (PLAYING/PAUSED/NONE) |
| `ktv:current_order:room:{roomId}` | String | Room's current order |
| `ktv:song:hot` | ZSet | Hot rankings (score=play count) |

## API Documentation

### Admin APIs (/api/admin)

> Requires JWT authentication, header: `Authorization: Bearer {token}`

| Module | Endpoint | Method | Description |
|--------|----------|--------|-------------|
| Auth | /login | POST | Admin login |
| Auth | /logout | POST | Logout |
| Songs | /songs | GET/POST | Song list/add |
| Songs | /songs/{id} | PUT/DELETE | Update/delete song |
| Singers | /singers | GET/POST | Singer list/add |
| Categories | /categories | GET/POST | Category list/add |
| Rooms | /rooms | GET/POST | Room list/add |
| Rooms | /rooms/{id}/status | PUT | Update room status |
| Orders | /orders | GET | Order list |
| Orders | /orders/open | POST | Open room |
| Orders | /orders/{id}/close | POST | Close order |

### Room APIs (/api/room)

> No JWT required, session identified by orderId

| Module | Endpoint | Method | Description |
|--------|----------|--------|-------------|
| Search | /songs/search | GET | Search songs (pinyin/name) |
| Search | /songs/by-singer/{id} | GET | Songs by singer |
| Search | /songs/by-category/{id} | GET | Songs by category |
| Search | /songs/hot | GET | Hot rankings |
| Queue | /{orderId}/queue/add | POST | Add song to queue |
| Queue | /{orderId}/queue/top/{id} | POST | Top song |
| Queue | /{orderId}/queue/remove/{id} | DELETE | Remove song |
| Queue | /{orderId}/queue | GET | Queue list |
| Queue | /{orderId}/played | GET | Played songs list |
| Control | /{orderId}/play/next | POST | Next song |
| Control | /{orderId}/play/replay | POST | Replay |
| Control | /{orderId}/play/pause | POST | Pause |
| Control | /{orderId}/play/resume | POST | Resume |
| Control | /{orderId}/play/current | GET | Current playback status |

### Media APIs (/api/media)

| Endpoint | Method | Description |
|----------|--------|-------------|
| /stream/{songId} | GET | Stream media file (Range supported) |
| /cover/{songId} | GET | Get cover image |
| /info/{songId} | GET | Get media info |

## Configuration

### Backend Config (application.yml)

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ktv?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password

  data:
    redis:
      host: localhost
      port: 6379

media:
  base-path: D:/ktv-media    # Media file storage path

jwt:
  secret: your-secret-key
  expiration: 7200000        # Token expiration 2 hours
```

### Frontend Proxy Config

Frontend dev server proxies `/api` requests to backend `http://localhost:8080` via Vite

## Development Standards

### Naming Conventions

- Table prefix: `t_`
- Field names: snake_case
- Java classes: PascalCase
- API endpoints: RESTful style

### Database Standards

- Logical delete: `deleted` field (0=active, 1=deleted)
- Required fields: `id`, `create_time`, `update_time`
- Charset: `utf8mb4`
- Foreign key relationships maintained at application layer

### Frontend Standards

- Functional components + Hooks (no Class components)
- Lazy loading routes
- Token storage: localStorage
- State persistence: Zustand persist
- Pagination: backend uses `current/size`

### Code Standards

- Follow Alibaba Java Coding Guidelines
- No business logic in Controller layer
- Service interface + Impl separation
- Unified response format `Result<T>` with code/msg/data

## Development Milestones

| Phase | Content | Status |
|-------|---------|--------|
| M1 - Setup | Backend skeleton, database, basic config | ✅ Completed |
| M2 - Admin Backend | CRUD APIs + JWT auth | ✅ Completed |
| M3 - Admin Frontend | admin-frontend React pages | ✅ Completed |
| M4 - Song Core Backend | Song search + queue + playback APIs | ✅ Completed |
| M5 - Order Management | Open/close room APIs + frontend | ✅ Completed |
| M6 - Redis Integration | Hot rankings, queue cache, state cache | ✅ Completed |
| M7 - Media Playback | Streaming, player integration | ✅ Completed |
| M8 - Room Frontend | room-frontend React pages | ✅ Completed |
| M9 - Code Review | Deep code review and optimization | ✅ Completed |
| M10 - Integration Test | Test data + API integration | 🚧 Pending |

## Documentation

- **[Project Overview](docs/project-overview.md)** - System architecture, tech selection, database design
- **[API Reference](docs/api-reference.md)** - Detailed API documentation
- **[Code Review Standards](docs/code-review-standards.md)** - Code quality standards
- **[Contributing Guide](CONTRIBUTING.md)** - How to contribute

## License

[MIT License](LICENSE)

## Author

**shaun.sheng**
