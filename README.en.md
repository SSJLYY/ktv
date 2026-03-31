# KTV Karaoke System

> Modern KTV management system based on Java 21 + Spring Boot 3 + React 18

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue)](https://reactjs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Introduction

KTV Karaoke System is a complete KTV venue management solution with two main modules: admin management system and room karaoke system. The system adopts a front-end and back-end separation architecture, supporting core business features such as song selection, queue management, playback control, and room management.

### Core Features

- **Admin Management**: Song management, singer management, category management, room management, order management, user permissions
- **Room Karaoke**: Multi-dimensional song search (pinyin/singer/category/hot), song queuing, playback control, history records
- **Media Playback**: Support for MP3/FLAC audio and MP4 video files with actual playback, streaming supports progress dragging
- **Redis Optimization**: Hot rankings, song caching, queue management

## Tech Stack

### Backend
- **Java 21** LTS (Virtual Threads support)
- **Spring Boot 3.2.x** (Jakarta EE 10)
- **MyBatis-Plus 3.5.7** (Simplified ORM)
- **MySQL 8.0** (Data storage)
- **Redis 6.x** (Cache/Queue)
- **JWT** (Stateless authentication)

### Frontend
- **React 18** + **Vite 5** (Build tool)
- **Ant Design 5** (Admin UI)
- **Ant Design Mobile 5** (Room UI)
- **Zustand** (Lightweight state management)
- **Axios** (HTTP client)
- **APlayer / react-player** (Media playback)

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
├── ktv-backend/              # Spring Boot backend
│   ├── src/main/java/com/ktv/
│   │   ├── config/            # Configuration classes
│   │   ├── controller/        # Controllers
│   │   │   ├── admin/         # Admin APIs
│   │   │   └── room/          # Room APIs
│   │   ├── service/           # Business logic
│   │   ├── mapper/            # MyBatis Mappers
│   │   ├── entity/            # Entity classes
│   │   ├── dto/               # Request DTOs
│   │   ├── vo/                # Response VOs
│   │   └── common/            # Common components
│   └── src/main/resources/
│       └── application.yml    # Configuration
│
├── admin-frontend/            # Admin frontend
│   ├── src/
│   │   ├── api/               # API wrappers
│   │   ├── pages/             # Page components
│   │   ├── components/        # Shared components
│   │   ├── store/             # State management
│   │   └── router/            # Router config
│   └── vite.config.js         # Vite config
│
├── room-frontend/             # Room frontend
│   └── (Same structure as admin-frontend)
│
├── sql/                       # Database scripts
│   ├── init-schema.sql        # Schema
│   └── init-data.sql          # Initial data
│
├── docs/                      # Documentation
│   ├── project-overview.md    # Project design doc
│   └── ai-memory-bank/        # AI task records
│
└── skills/                    # Personal skills (local dev)
```

## Core Features

### Song Search
- **Pinyin Search**: Support pinyin initials and full pinyin search
- **Singer Search**: View songs by singer
- **Category Browse**: Browse by language, genre
- **Hot Rankings**: Real-time ranking based on play count

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

## API Documentation

### Admin APIs (/api/admin)

| Endpoint | Method | Description |
|----------|--------|-------------|
| /login | POST | Admin login |
| /logout | POST | Logout |
| /songs | GET/POST | Song list/add |
| /songs/{id} | PUT/DELETE | Update/delete song |
| /singers | GET/POST | Singer list/add |
| /categories | GET/POST | Category list/add |
| /rooms | GET/POST | Room list/add |
| /rooms/{id}/status | PUT | Update room status |
| /orders | GET | Order list |
| /orders/open | POST | Open room |
| /orders/{id}/close | POST | Close order |

### Room APIs (/api/room)

| Endpoint | Method | Description |
|----------|--------|-------------|
| /songs/search | GET | Search songs |
| /songs/by-singer/{id} | GET | Search by singer |
| /songs/by-category/{id} | GET | Search by category |
| /songs/hot | GET | Hot rankings |
| /{orderId}/queue/add | POST | Add song |
| /{orderId}/queue/top/{id} | POST | Top song |
| /{orderId}/queue/remove/{id} | DELETE | Remove song |
| /{orderId}/queue | GET | Queue list |
| /{orderId}/played | GET | Played list |
| /{orderId}/play/next | POST | Next song |
| /{orderId}/play/replay | POST | Replay |
| /{orderId}/play/pause | POST | Pause |
| /{orderId}/play/resume | POST | Resume |
| /{orderId}/play/current | GET | Current status |

### Media APIs (/api/media)

| Endpoint | Method | Description |
|----------|--------|-------------|
| /stream/{songId} | GET | Stream media file |
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

### Frontend Standards
- Functional components + Hooks
- Lazy loading routes
- Token storage: localStorage
- State persistence: Zustand persist

## Documentation

For complete project design documentation, see [docs/project-overview.md](docs/project-overview.md)

## License

MIT License

## Author

shaun.sheng
