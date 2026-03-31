# KTV点歌系统

> 基于Java 21 + Spring Boot 3 + React 18 的现代化KTV点歌管理系统

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue)](https://reactjs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 项目简介

KTV点歌系统是一套完整的KTV门店管理解决方案，包含后台管理系统和包厢点歌系统两大模块。系统采用前后端分离架构，支持歌曲点播、排队管理、播放控制、包厢管理等核心业务。

### 核心功能

- **后台管理**：歌曲管理、歌手管理、分类管理、包厢管理、订单管理、用户权限
- **包厢点歌**：多维度歌曲检索（拼音/歌手/分类/热门）、点歌排队、播放控制、历史记录
- **媒体播放**：支持MP3/FLAC音频和MP4视频文件的实际播放，流式传输支持进度拖拽
- **Redis优化**：热门排行、歌曲缓存、队列管理

## 技术栈

### 后端
- **Java 21** LTS（支持虚拟线程）
- **Spring Boot 3.2.x**（Jakarta EE 10）
- **MyBatis-Plus 3.5.7**（简化ORM操作）
- **MySQL 8.0**（数据存储）
- **Redis 6.x**（缓存/队列）
- **JWT**（无状态认证）

### 前端
- **React 18** + **Vite 5**（构建工具）
- **Ant Design 5**（后台管理界面）
- **Ant Design Mobile 5**（包厢点歌界面）
- **Zustand**（轻量状态管理）
- **Axios**（HTTP客户端）
- **APlayer / react-player**（媒体播放）

## 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- MySQL 8.0+
- Redis 6.x+
- Maven 3.6.3+

### 数据库初始化

```bash
# 创建数据库
CREATE DATABASE ktv CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 导入表结构
mysql -u root -p ktv < sql/init-schema.sql

# 导入初始数据
mysql -u root -p ktv < sql/init-data.sql
```

### 后端启动

```bash
cd ktv-backend

# 修改配置文件
# src/main/resources/application.yml
# 配置数据库连接、Redis连接、媒体文件路径

# 启动项目
mvn spring-boot:run
```

后端默认端口：`http://localhost:8080`

### 前端启动

#### 后台管理端（admin-frontend）

```bash
cd admin-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

访问地址：`http://localhost:3000`
默认账号：`admin` / `admin123`

#### 包厢点歌端（room-frontend）

```bash
cd room-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

访问地址：`http://localhost:3001`

## 项目结构

```
ktv/
├── ktv-backend/              # Spring Boot 后端
│   ├── src/main/java/com/ktv/
│   │   ├── config/            # 配置类
│   │   ├── controller/        # 控制器
│   │   │   ├── admin/         # 后台管理接口
│   │   │   └── room/          # 包厢端接口
│   │   ├── service/           # 业务逻辑
│   │   ├── mapper/            # MyBatis Mapper
│   │   ├── entity/            # 实体类
│   │   ├── dto/               # 请求参数
│   │   ├── vo/                # 响应参数
│   │   └── common/            # 公共组件
│   └── src/main/resources/
│       └── application.yml    # 配置文件
│
├── admin-frontend/            # 后台管理前端
│   ├── src/
│   │   ├── api/               # API封装
│   │   ├── pages/             # 页面组件
│   │   ├── components/        # 公共组件
│   │   ├── store/             # 状态管理
│   │   └── router/            # 路由配置
│   └── vite.config.js         # Vite配置
│
├── room-frontend/             # 包厢点歌前端
│   └── (结构同admin-frontend)
│
├── sql/                       # 数据库脚本
│   ├── init-schema.sql        # 表结构
│   └── init-data.sql          # 初始数据
│
├── docs/                      # 项目文档
│   ├── project-overview.md    # 项目设计文档
│   └── ai-memory-bank/       # AI任务记录
│
└── skills/                    # 个人技能目录（本地开发）
```

## 核心功能说明

### 歌曲检索

- **拼音搜索**：支持拼音首字母和全拼搜索
- **歌手点歌**：按歌手查看歌曲列表
- **分类浏览**：按语种、风格分类
- **热门排行**：基于点播次数的实时排行榜

### 点歌队列

- 使用 Redis List 管理点歌队列
- 支持置顶、取消、调整顺序
- 队列与数据库双写保证数据一致性

### 播放控制

- **播放/暂停**：控制当前歌曲播放状态
- **切歌**：跳过当前歌曲，播放下一首
- **重唱**：重新播放当前歌曲
- 支持音频（MP3/FLAC）和视频（MP4）文件

### 媒体文件

- **上传管理**：后台支持上传音视频文件
- **流式传输**：支持 HTTP Range 请求，实现进度拖拽
- **自动识别**：根据文件后缀自动选择播放器

## API 接口

### 后台管理接口（/api/admin）

| 接口 | 方法 | 说明 |
|------|------|------|
| /login | POST | 管理员登录 |
| /logout | POST | 退出登录 |
| /songs | GET/POST | 歌曲列表/新增 |
| /songs/{id} | PUT/DELETE | 修改/删除歌曲 |
| /singers | GET/POST | 歌手列表/新增 |
| /categories | GET/POST | 分类列表/新增 |
| /rooms | GET/POST | 包厢列表/新增 |
| /rooms/{id}/status | PUT | 更新包厢状态 |
| /orders | GET | 订单列表 |
| /orders/open | POST | 开台 |
| /orders/{id}/close | POST | 结账 |

### 包厢端接口（/api/room）

| 接口 | 方法 | 说明 |
|------|------|------|
| /songs/search | GET | 搜索歌曲 |
| /songs/by-singer/{id} | GET | 按歌手查询 |
| /songs/by-category/{id} | GET | 按分类查询 |
| /songs/hot | GET | 热门排行 |
| /{orderId}/queue/add | POST | 点歌 |
| /{orderId}/queue/top/{id} | POST | 置顶 |
| /{orderId}/queue/remove/{id} | DELETE | 取消点歌 |
| /{orderId}/queue | GET | 已点列表 |
| /{orderId}/played | GET | 已唱列表 |
| /{orderId}/play/next | POST | 切歌 |
| /{orderId}/play/replay | POST | 重唱 |
| /{orderId}/play/pause | POST | 暂停 |
| /{orderId}/play/resume | POST | 恢复 |
| /{orderId}/play/current | GET | 当前播放状态 |

### 媒体接口（/api/media）

| 接口 | 方法 | 说明 |
|------|------|------|
| /stream/{songId} | GET | 流式传输音视频文件 |
| /cover/{songId} | GET | 获取封面图 |
| /info/{songId} | GET | 获取媒体信息 |

## 配置说明

### 后端配置（application.yml）

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
  base-path: D:/ktv-media    # 媒体文件存储路径

jwt:
  secret: your-secret-key
  expiration: 7200000        # Token有效期2小时
```

### 前端代理配置

前端开发服务器通过 Vite 代理将 `/api` 请求转发到后端 `http://localhost:8080`

## 开发规范

### 命名规范
- 表名前缀：`t_`
- 字段名：下划线命名
- Java类：PascalCase
- API接口：RESTful风格

### 数据库规范
- 逻辑删除：`deleted` 字段（0未删 1已删）
- 必备字段：`id`、`create_time`、`update_time`
- 字符集：`utf8mb4`

### 前端规范
- 函数组件 + Hooks
- 路由懒加载
- Token存储：localStorage
- 状态持久化：Zustand persist

## 文档

完整的项目设计文档请查看 [docs/project-overview.md](docs/project-overview.md)

## License

MIT License

## 作者

shaun.sheng
