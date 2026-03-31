# KTV点歌系统

> 基于 Java 21 + Spring Boot 3 + React 18 的现代化KTV点歌管理系统

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-green)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue)](https://reactjs.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 项目简介

KTV点歌系统是一套完整的KTV门店管理解决方案，包含后台管理系统和包厢点歌系统两大模块。系统采用前后端分离架构，支持歌曲点播、排队管理、播放控制、包厢管理等核心业务。

### 核心功能

- **后台管理**：歌曲管理、歌手管理、分类管理、包厢管理、订单管理、用户权限
- **包厢点歌**：多维度歌曲检索（拼音/歌手/分类/热门）、点歌排队、播放控制、历史记录
- **媒体播放**：支持 MP3/FLAC 音频和 MP4 视频文件的实际播放，流式传输支持进度拖拽
- **Redis优化**：热门排行、歌曲缓存、队列管理、播放状态同步

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 LTS | 支持虚拟线程（Virtual Threads） |
| Spring Boot | 3.2.x | Jakarta EE 10 |
| MyBatis-Plus | 3.5.7 | 简化ORM操作 |
| MySQL | 8.0 | 主数据存储 |
| Redis | 6.x+ | 缓存/队列/热门排行 |
| JWT | 无状态认证 | 前后端分离架构 |

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| React | 18 | 函数组件 + Hooks |
| Vite | 5.x | 构建工具 |
| Ant Design | 5.x | 后台管理界面 |
| Ant Design Mobile | 5.x | 包厢点歌界面 |
| Zustand | 4.x | 轻量状态管理 |
| Axios | 1.x | HTTP客户端 |

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
├── ktv-backend/                    # Spring Boot 后端
│   ├── src/main/java/com/ktv/
│   │   ├── config/                 # 配置类
│   │   │   ├── CorsConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   ├── MyBatisPlusConfig.java
│   │   │   └── WebMvcConfig.java
│   │   ├── controller/             # 控制器
│   │   │   ├── admin/              # 后台管理接口 /api/admin/**
│   │   │   └── room/               # 包厢端接口 /api/room/**
│   │   ├── service/                # 业务逻辑
│   │   │   └── impl/
│   │   ├── mapper/                 # MyBatis Mapper
│   │   ├── entity/                 # 实体类
│   │   ├── dto/                    # 请求参数
│   │   ├── vo/                     # 响应参数
│   │   ├── common/                 # 公共组件
│   │   │   ├── result/             # 统一返回格式
│   │   │   ├── exception/          # 全局异常处理
│   │   │   └── enums/              # 枚举类
│   │   ├── interceptor/            # 拦截器
│   │   ├── task/                   # 定时任务
│   │   └── util/                   # 工具类
│   └── src/main/resources/
│       └── application.yml
│
├── admin-frontend/                 # 后台管理前端
│   ├── src/
│   │   ├── api/                    # API封装
│   │   ├── pages/                  # 页面组件
│   │   │   ├── Login/              # 登录页
│   │   │   ├── Song/               # 歌曲管理
│   │   │   ├── Singer/             # 歌手管理
│   │   │   ├── Category/           # 分类管理
│   │   │   ├── Room/               # 包厢管理
│   │   │   └── Order/              # 订单管理
│   │   ├── components/             # 公共组件
│   │   ├── layouts/                # 布局组件
│   │   ├── store/                  # Zustand状态管理
│   │   ├── router/                 # React Router v6
│   │   └── utils/                  # 工具函数
│   └── vite.config.js
│
├── room-frontend/                  # 包厢点歌前端
│   ├── src/
│   │   ├── api/                    # API封装
│   │   ├── pages/                  # 页面组件
│   │   │   ├── Join/               # 加入房间
│   │   │   ├── Search/             # 歌曲检索
│   │   │   └── Queue/              # 点歌队列
│   │   ├── components/             # 公共组件
│   │   │   ├── PlayBar/            # 底部播放控制栏
│   │   │   ├── SongCard/           # 歌曲卡片
│   │   │   └── TabBar/             # 底部导航栏
│   │   ├── layouts/                # 布局组件
│   │   ├── store/                  # Zustand状态管理
│   │   ├── router/                 # React Router v6
│   │   └── assets/                 # 静态资源
│   └── vite.config.js
│
├── sql/                            # 数据库脚本
│   ├── init-schema.sql             # 表结构
│   └── init-data.sql               # 初始数据
│
├── docs/                           # 项目文档
│   ├── README.md                   # 文档索引
│   ├── project-overview.md         # 项目设计文档
│   ├── api-reference.md            # API参考文档
│   ├── code-review-standards.md    # 代码审查规范
│   └── tasks/                      # 任务文档
│
├── skills/                         # 开发辅助技能
│
├── README.md                       # 项目说明
├── README.en.md                    # 英文说明
├── CONTRIBUTING.md                 # 贡献指南
└── LICENSE                         # MIT许可证
```

## 核心功能说明

### 歌曲检索

- **拼音搜索**：支持拼音首字母和全拼搜索
- **歌手点歌**：按歌手查看歌曲列表
- **分类浏览**：按语种、风格分类
- **热门排行**：基于点播次数的实时排行榜（Redis ZSet）

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

### Redis 数据结构

| Key格式 | 类型 | 说明 |
|---------|------|------|
| `ktv:queue:{orderId}` | List | 点歌队列 |
| `ktv:playing:{orderId}` | String | 当前播放歌曲ID |
| `ktv:play:status:{orderId}` | String | 播放状态（PLAYING/PAUSED/NONE） |
| `ktv:current_order:room:{roomId}` | String | 包厢当前订单 |
| `ktv:song:hot` | ZSet | 热门排行（score=点播次数） |

## API 接口

### 后台管理接口（/api/admin）

> 需要JWT认证，请求头携带 `Authorization: Bearer {token}`

| 模块 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 认证 | /login | POST | 管理员登录 |
| 认证 | /logout | POST | 退出登录 |
| 歌曲 | /songs | GET/POST | 歌曲列表/新增 |
| 歌曲 | /songs/{id} | PUT/DELETE | 修改/删除歌曲 |
| 歌手 | /singers | GET/POST | 歌手列表/新增 |
| 分类 | /categories | GET/POST | 分类列表/新增 |
| 包厢 | /rooms | GET/POST | 包厢列表/新增 |
| 包厢 | /rooms/{id}/status | PUT | 更新包厢状态 |
| 订单 | /orders | GET | 订单列表 |
| 订单 | /orders/open | POST | 开台 |
| 订单 | /orders/{id}/close | POST | 结账 |

### 包厢端接口（/api/room）

> 无需JWT认证，通过orderId标识会话

| 模块 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 检索 | /songs/search | GET | 搜索歌曲（拼音/名称） |
| 检索 | /songs/by-singer/{id} | GET | 按歌手查歌 |
| 检索 | /songs/by-category/{id} | GET | 按分类查歌 |
| 检索 | /songs/hot | GET | 热门排行 |
| 点歌 | /{orderId}/queue/add | POST | 点歌（加入队列） |
| 点歌 | /{orderId}/queue/top/{id} | POST | 置顶歌曲 |
| 点歌 | /{orderId}/queue/remove/{id} | DELETE | 取消点歌 |
| 点歌 | /{orderId}/queue | GET | 已点列表 |
| 点歌 | /{orderId}/played | GET | 已唱列表 |
| 控制 | /{orderId}/play/next | POST | 切歌 |
| 控制 | /{orderId}/play/replay | POST | 重唱 |
| 控制 | /{orderId}/play/pause | POST | 暂停 |
| 控制 | /{orderId}/play/resume | POST | 恢复播放 |
| 控制 | /{orderId}/play/current | GET | 当前播放状态 |

### 媒体接口（/api/media）

| 接口 | 方法 | 说明 |
|------|------|------|
| /stream/{songId} | GET | 流式传输音视频文件（支持Range） |
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
- 字段名：下划线命名（snake_case）
- Java类：PascalCase
- API接口：RESTful风格

### 数据库规范

- 逻辑删除：`deleted` 字段（0未删 1已删）
- 必备字段：`id`、`create_time`、`update_time`
- 字符集：`utf8mb4`
- 外键关系在应用层维护，不使用物理外键

### 前端规范

- 函数组件 + Hooks，禁止 Class 组件
- 路由懒加载
- Token存储：localStorage
- 状态持久化：Zustand persist
- 分页参数：后端使用 `current/size`

### 代码规范

- 遵循阿里巴巴Java开发手册
- Controller层不写业务逻辑
- Service接口 + Impl实现分离
- 统一返回格式 `Result<T>` 包含 code/msg/data

## 开发里程碑

| 阶段 | 内容 | 状态 |
|------|------|------|
| M1 - 基础搭建 | 后端骨架、数据库、基础配置 | ✅ 已完成 |
| M2 - 后台管理后端 | 歌曲/歌手/分类/包厢CRUD API + JWT登录 | ✅ 已完成 |
| M3 - 后台管理前端 | admin-frontend React页面 | ✅ 已完成 |
| M4 - 点歌核心后端 | 歌曲检索 + 点歌排队 + 播放控制 API | ✅ 已完成 |
| M5 - 订单管理 | 开台/结账API + 前端页面 | ✅ 已完成 |
| M6 - Redis集成 | 热门排行、队列缓存、状态缓存 | ✅ 已完成 |
| M7 - 媒体播放 | 流式传输、播放器集成 | ✅ 已完成 |
| M8 - room-frontend | 包厢点歌React页面 | ✅ 已完成 |
| M9 - 代码审查 | 深度代码审查与优化 | ✅ 已完成 |
| M10 - 联调测试 | 初始化测试数据 + 接口联调 | 🚧 待进行 |

## 文档

- **[项目概览](docs/project-overview.md)** - 系统架构、技术选型、数据库设计
- **[API参考](docs/api-reference.md)** - 详细接口文档
- **[代码审查规范](docs/code-review-standards.md)** - 代码质量标准
- **[贡献指南](CONTRIBUTING.md)** - 如何参与开发

## License

[MIT License](LICENSE)

## 作者

**shaun.sheng**
