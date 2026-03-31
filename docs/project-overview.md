# KTV点歌系统 - 项目设计文档

> 版本：v1.3
> 创建日期：2026-03-30  更新：2026-03-30（前端改为React；JDK升级21，Spring Boot升级3.x；纳入本地文件流媒体播放）
> 作者：shaun.sheng
> 技术栈：Java 21 + Spring Boot 3.x + MySQL 8 + MyBatis-Plus + Redis + React 18 + Ant Design

---

## 一、项目概述

### 1.1 项目背景
构建一套KTV点歌管理系统，支持包厢管理、歌曲点播、排队播放、歌手/歌曲检索等核心业务场景。系统面向KTV门店运营，提供后台管理 + 包厢端点歌两大模块。

### 1.2 项目目标
| 目标 | 说明 |
|------|------|
| 核心功能 | 歌曲管理、点歌排队、包厢管理、播放控制 |
| 用户体验 | 快速检索歌曲、流畅的点歌/切歌操作 |
| 系统性能 | 利用Redis缓存热门歌曲和排队列表，保证响应速度 |
| 可维护性 | 清晰的分层架构，便于后续扩展 |

### 1.3 项目范围

**包含（In Scope）：**
- 后台管理系统（歌曲管理、歌手管理、包厢管理、订单管理）
- 包厢端点歌系统（点歌、排队、播放控制）
- 用户/权限基础管理

**不包含（Out of Scope）：**
- 在线支付对接
- 移动端APP
- 第三方音乐平台API对接（QQ音乐/网易云等，预留扩展，本期不实现）

**包含（已调整纳入范围）：**
- 本地音视频文件实际播放（MP3/FLAC/MP4），Spring Boot 提供 Range 流式接口，前端 APlayer/react-player 播放

---

## 二、技术架构

### 2.1 技术栈选型

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java | 21 | JDK 21 LTS，支持虚拟线程（Virtual Threads） |
| 框架 | Spring Boot | 3.2.x | 最新稳定版，原生支持JDK21虚拟线程、Jakarta EE 10 |
| ORM | MyBatis-Plus | 3.5.x | 简化CRUD开发，兼容Spring Boot 3 |
| 数据库 | MySQL | 8.0 | 主数据存储 |
| 缓存 | Redis | 6.x+ | 热门歌曲缓存、点歌排队队列 |
| 后台前端 | React + Ant Design | 18.x / 5.x | 独立项目 `admin-frontend`，后台管理界面 |
| 包厢端前端 | React + Ant Design Mobile | 18.x / 5.x | 独立项目 `room-frontend`，触摸屏点歌界面 |
| 前端路由 | React Router | v6 | 单页路由管理 |
| 前端状态 | Zustand | 4.x | 轻量状态管理 |
| HTTP客户端 | Axios | 1.x | 前后端通信 |
| 前端构建 | Vite | 5.x | 快速开发构建 |
| 后端构建 | Maven | 3.9+ | 项目构建管理（Spring Boot 3 要求Maven 3.6.3+） |
| **前端播放器** | **APlayer / react-player** | latest | 音频/视频播放组件，支持MP3/FLAC/MP4 |
| **流媒体接口** | Spring Boot Resource/Range | - | 后端支持 HTTP Range 请求，实现拖拽进度条 |

### 2.2 系统架构图

```
┌──────────────────────────────────────────────────────────────────┐
│                          客户端层                                  │
│  ┌────────────────────────┐    ┌──────────────────────────────┐   │
│  │   后台管理端            │    │   包厢点歌端                  │   │
│  │   admin-frontend       │    │   room-frontend              │   │
│  │   React 18 + AntD 5    │    │   React 18 + AntD Mobile 5   │   │
│  │   Vite + Axios         │    │   Vite + Axios               │   │
│  │   :3000                │    │   APlayer / react-player     │   │
│  └──────────┬─────────────┘    │   :3001                      │   │
│             │                  └──────────────┬───────────────┘   │
│             │  HTTP/JSON (CORS)               │                   │
│             │              HTTP Range 流媒体   │                   │
├─────────────┼─────────────────────────────────┼───────────────────┤
│             ▼                                 ▼                   │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │                Spring Boot REST API (:8080)               │    │
│  │   ┌────────────┐  ┌────────────┐  ┌──────────────────┐   │    │
│  │   │ Controller │  │  Service   │  │  Common          │   │    │
│  │   │  /api/**   │  │    层      │  │  (Result/异常)   │   │    │
│  │   └─────┬──────┘  └─────┬──────┘  └──────────────────┘   │    │
│  │         └───────────────┘                                 │    │
│  │   ┌─────────────────────────────────────────────────┐     │    │
│  │   │  MediaStreamController /api/media/**             │     │    │
│  │   │  支持 HTTP Range，ResourceRegion 分块传输         │     │    │
│  │   └─────────────────────┬───────────────────────────┘     │    │
│  │                    ▼    ▼                                  │    │
│  │         ┌──────────────────────┐                         │    │
│  │         │  MyBatis DAO/Mapper  │                         │    │
│  │         └──────┬───────────────┘                         │    │
│  └────────────────┼────────────────────────────────────────┘    │
│                   │                                               │
├───────────────────┼───────────────────────────────────────────────┤
│          ┌────────┴─────────┐   ┌──────────┐  ┌───────────────┐  │
│          │    MySQL 8.0     │   │  Redis   │  │ 本地磁盘/NAS  │  │
│          │  (主数据存储)     │   │ (缓存/队列)│  │ 音视频文件    │  │
│          └──────────────────┘   └──────────┘  └───────────────┘  │
│                              数据层                               │
└──────────────────────────────────────────────────────────────────┘
```

### 2.3 项目目录结构

```
ktv/
├── ktv-backend/                    # Spring Boot 后端（REST API）
│   ├── src/main/java/com/ktv/
│   │   ├── config/                 # 配置类（Redis、MyBatis、CORS等）
│   │   ├── controller/
│   │   │   ├── admin/              # 后台管理接口 /api/admin/**
│   │   │   └── room/               # 包厢端接口 /api/room/**
│   │   ├── service/
│   │   │   └── impl/
│   │   ├── mapper/
│   │   ├── entity/
│   │   ├── dto/                    # 请求入参
│   │   ├── vo/                     # 响应出参
│   │   └── common/
│   │       ├── result/             # Result<T> 统一返回
│   │       ├── exception/          # 全局异常处理
│   │       └── util/               # 工具类
│   └── pom.xml
│
├── admin-frontend/                 # 后台管理 React 项目
│   ├── src/
│   │   ├── api/                    # Axios 接口封装
│   │   ├── pages/                  # 页面组件
│   │   │   ├── Login/
│   │   │   ├── Song/               # 歌曲管理
│   │   │   ├── Singer/             # 歌手管理
│   │   │   ├── Room/               # 包厢管理
│   │   │   └── Order/              # 订单管理
│   │   ├── components/             # 公共组件
│   │   ├── store/                  # 状态管理（Zustand）
│   │   ├── router/                 # React Router v6
│   │   └── utils/
│   ├── vite.config.js
│   └── package.json
│
├── room-frontend/                  # 包厢点歌 React 项目
│   ├── src/
│   │   ├── api/
│   │   ├── pages/
│   │   │   ├── Search/             # 歌曲检索
│   │   │   ├── Queue/              # 已点/已唱列表
│   │   │   └── PlayControl/        # 播放控制
│   │   ├── components/
│   │   ├── store/
│   │   └── utils/
│   ├── vite.config.js
│   └── package.json
│
├── sql/                            # 数据库脚本
│   ├── init-schema.sql
│   └── init-data.sql
└── docs/                           # 项目文档
    └── project-overview.md
```

---

## 三、功能模块设计

### 3.1 模块总览

```
KTV点歌系统
├── 🔧 后台管理模块
│   ├── 歌曲管理（CRUD、分类、导入）
│   ├── 歌手管理（CRUD、关联歌曲）
│   ├── 包厢管理（CRUD、状态管理）
│   ├── 订单管理（开台、结账、查询）
│   └── 系统管理（用户、角色、权限）
│
└── 🎤 包厢点歌模块
    ├── 歌曲检索（拼音、歌手、分类、热度）
    ├── 点歌操作（点歌、置顶、删除）
    ├── 播放控制（播放、暂停、切歌、重唱）
    ├── 已点列表（排队展示、顺序调整）
    └── 已唱列表（历史记录）
```

### 3.2 后台管理模块

#### 3.2.1 歌曲管理
| 功能 | 说明 | 优先级 |
|------|------|--------|
| 歌曲列表 | 分页查询，支持按名称/歌手/分类筛选 | P0 |
| 新增歌曲 | 录入歌曲名、歌手、分类、拼音首字母、语种等 | P0 |
| 编辑歌曲 | 修改歌曲信息 | P0 |
| 删除歌曲 | 软删除 | P0 |
| 歌曲分类管理 | 管理歌曲分类（流行、经典、摇滚、民谣等） | P1 |

#### 3.2.2 歌手管理
| 功能 | 说明 | 优先级 |
|------|------|--------|
| 歌手列表 | 分页查询，支持按名称/拼音/地区筛选 | P0 |
| 新增/编辑/删除歌手 | 基本CRUD | P0 |
| 歌手关联歌曲 | 查看某歌手下所有歌曲 | P1 |

#### 3.2.3 包厢管理
| 功能 | 说明 | 优先级 |
|------|------|--------|
| 包厢列表 | 查看所有包厢及状态（空闲/使用中/清洁中） | P0 |
| 新增/编辑/删除包厢 | 包厢名称、类型（小/中/大/豪华）、费用 | P0 |
| 包厢状态管理 | 开台/结账/清洁等状态流转 | P0 |

#### 3.2.4 订单管理
| 功能 | 说明 | 优先级 |
|------|------|--------|
| 开台 | 选择包厢，创建消费订单 | P0 |
| 结账 | 根据时长计算费用，关闭订单 | P0 |
| 订单查询 | 按日期/包厢/状态查询历史订单 | P1 |

#### 3.2.5 系统管理
| 功能 | 说明 | 优先级 |
|------|------|--------|
| 用户管理 | 管理员账号CRUD | P1 |
| 登录/登出 | 基于Session或JWT的简单认证 | P0 |
| 角色权限 | 基础的角色划分（超级管理员/普通管理员） | P2 |

### 3.3 包厢点歌模块

#### 3.3.1 歌曲检索
| 功能 | 说明 | 优先级 |
|------|------|--------|
| 拼音搜索 | 输入拼音首字母快速匹配歌曲 | P0 |
| 歌手点歌 | 先选歌手，再选歌曲 | P0 |
| 分类浏览 | 按语种、风格等分类浏览 | P0 |
| 热门排行 | 按点播次数排序（Redis缓存） | P1 |
| 新歌推荐 | 按上架时间倒序 | P2 |

#### 3.3.2 点歌与排队
| 功能 | 说明 | 优先级 |
|------|------|--------|
| 点歌 | 将歌曲加入当前包厢排队列表尾部 | P0 |
| 置顶 | 将某首歌移到队列最前（下一首播放） | P0 |
| 删除 | 从排队列表移除某首歌 | P0 |
| 已点列表 | 展示当前排队中的歌曲列表 | P0 |
| 已唱列表 | 展示已经播放完的歌曲 | P1 |

#### 3.3.3 播放控制
| 功能 | 说明 | 优先级 |
|------|------|--------|
| 播放/暂停 | 控制当前歌曲播放状态 | P0 |
| 切歌 | 跳过当前歌曲，播放下一首 | P0 |
| 重唱 | 重新播放当前歌曲 | P0 |
| 播放状态展示 | 显示当前播放歌曲信息和进度（模拟） | P1 |

---

## 四、数据库设计

### 4.1 ER关系概览

```
[歌手 Singer] 1───N [歌曲 Song] N───1 [歌曲分类 Category]
                         │
                         N
                         │
[包厢 Room] 1──N [订单 Order] 1──N [点歌记录 OrderSong]
                                        │
                                        │
                                   [播放队列 PlayQueue]
                                   (Redis管理)

[用户 SysUser] ──── [角色 SysRole]
```

### 4.2 表结构设计

#### 4.2.1 歌手表 `t_singer`
```sql
CREATE TABLE `t_singer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '歌手名',
  `pinyin` VARCHAR(200) DEFAULT NULL COMMENT '拼音全拼',
  `pinyin_initial` VARCHAR(50) DEFAULT NULL COMMENT '拼音首字母',
  `gender` TINYINT DEFAULT 0 COMMENT '性别：0未知 1男 2女 3组合',
  `region` VARCHAR(50) DEFAULT NULL COMMENT '地区：内地/港台/欧美/日韩/其他',
  `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `song_count` INT DEFAULT 0 COMMENT '歌曲数量（冗余字段）',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  INDEX `idx_pinyin_initial` (`pinyin_initial`),
  INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌手表';
```

#### 4.2.2 歌曲分类表 `t_category`
```sql
CREATE TABLE `t_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `sort_order` INT DEFAULT 0 COMMENT '排序序号',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌曲分类表';
```

#### 4.2.3 歌曲表 `t_song`
```sql
CREATE TABLE `t_song` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(200) NOT NULL COMMENT '歌曲名',
  `singer_id` BIGINT NOT NULL COMMENT '歌手ID',
  `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
  `pinyin` VARCHAR(500) DEFAULT NULL COMMENT '拼音全拼',
  `pinyin_initial` VARCHAR(100) DEFAULT NULL COMMENT '拼音首字母',
  `language` VARCHAR(20) DEFAULT '国语' COMMENT '语种：国语/粤语/英语/日语/韩语/其他',
  `duration` INT DEFAULT 0 COMMENT '时长（秒）',
  `file_path` VARCHAR(500) DEFAULT NULL COMMENT '歌曲文件路径',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
  `play_count` INT DEFAULT 0 COMMENT '总点播次数',
  `is_hot` TINYINT DEFAULT 0 COMMENT '是否热门：0否 1是',
  `is_new` TINYINT DEFAULT 0 COMMENT '是否新歌：0否 1是',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0下架 1上架',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  INDEX `idx_singer_id` (`singer_id`),
  INDEX `idx_category_id` (`category_id`),
  INDEX `idx_pinyin_initial` (`pinyin_initial`),
  INDEX `idx_play_count` (`play_count` DESC),
  INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌曲表';
```

#### 4.2.4 包厢表 `t_room`
```sql
CREATE TABLE `t_room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '包厢名称（如：A01、豪华1号）',
  `type` VARCHAR(20) NOT NULL COMMENT '类型：小包/中包/大包/豪华包',
  `capacity` INT DEFAULT 0 COMMENT '容纳人数',
  `price_per_hour` DECIMAL(10,2) DEFAULT 0.00 COMMENT '每小时价格',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0空闲 1使用中 2清洁中 3维修中',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='包厢表';
```

#### 4.2.5 订单表 `t_order`
```sql
CREATE TABLE `t_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号',
  `room_id` BIGINT NOT NULL COMMENT '包厢ID',
  `start_time` DATETIME NOT NULL COMMENT '开台时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `duration_minutes` INT DEFAULT 0 COMMENT '消费时长（分钟）',
  `total_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '总费用',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1消费中 2已结账 3已取消',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作员ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  INDEX `idx_room_id` (`room_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费订单表';
```

#### 4.2.6 点歌记录表 `t_order_song`
```sql
CREATE TABLE `t_order_song` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `song_id` BIGINT NOT NULL COMMENT '歌曲ID',
  `song_name` VARCHAR(200) DEFAULT NULL COMMENT '歌曲名（冗余）',
  `singer_name` VARCHAR(100) DEFAULT NULL COMMENT '歌手名（冗余）',
  `sort_order` INT DEFAULT 0 COMMENT '排序序号',
  `status` TINYINT DEFAULT 0 COMMENT '状态：0等待 1播放中 2已播放 3已跳过',
  `play_time` DATETIME DEFAULT NULL COMMENT '开始播放时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点歌时间',
  PRIMARY KEY (`id`),
  INDEX `idx_order_id` (`order_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点歌记录表';
```

#### 4.2.7 系统用户表 `t_sys_user`
```sql
CREATE TABLE `t_sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(200) NOT NULL COMMENT '密码（加密存储）',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `role` VARCHAR(20) DEFAULT 'admin' COMMENT '角色：super_admin/admin',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';
```

### 4.3 Redis数据结构设计

| Key格式 | 类型 | 说明 | 过期时间 |
|---------|------|------|----------|
| `ktv:queue:{orderId}` | List | 包厢点歌排队队列，存储orderSong的ID列表 | 随订单结束清除 |
| `ktv:playing:{orderId}` | String | 当前正在播放的歌曲orderSongId | 随订单结束清除 |
| `ktv:song:hot` | ZSet | 热门歌曲排行榜，score=点播次数 | 24小时 |
| `ktv:song:cache:{songId}` | Hash | 歌曲详情缓存 | 1小时 |
| `ktv:room:status` | Hash | 所有包厢状态快照 | 不过期，实时更新 |

---

## 五、接口设计概要

### 5.1 后台管理接口

| 模块 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 登录 | `/api/admin/login` | POST | 管理员登录 |
| 登录 | `/api/admin/logout` | POST | 退出登录 |
| 歌曲 | `/api/admin/songs` | GET | 歌曲分页列表 |
| 歌曲 | `/api/admin/songs` | POST | 新增歌曲 |
| 歌曲 | `/api/admin/songs/{id}` | PUT | 修改歌曲 |
| 歌曲 | `/api/admin/songs/{id}` | DELETE | 删除歌曲 |
| 歌手 | `/api/admin/singers` | GET/POST/PUT/DELETE | 歌手CRUD |
| 分类 | `/api/admin/categories` | GET/POST/PUT/DELETE | 分类CRUD |
| 包厢 | `/api/admin/rooms` | GET/POST/PUT/DELETE | 包厢CRUD |
| 包厢 | `/api/admin/rooms/{id}/status` | PUT | 更新包厢状态 |
| 订单 | `/api/admin/orders` | GET | 订单列表 |
| 订单 | `/api/admin/orders/open` | POST | 开台 |
| 订单 | `/api/admin/orders/{id}/close` | POST | 结账 |

### 5.2 包厢点歌接口

| 模块 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 检索 | `/api/room/songs/search` | GET | 搜索歌曲（拼音/名称） |
| 检索 | `/api/room/songs/by-singer/{singerId}` | GET | 按歌手查歌 |
| 检索 | `/api/room/songs/by-category/{categoryId}` | GET | 按分类查歌 |
| 检索 | `/api/room/songs/hot` | GET | 热门排行 |
| 点歌 | `/api/room/{orderId}/queue/add` | POST | 点歌（加入队列） |
| 点歌 | `/api/room/{orderId}/queue/top/{orderSongId}` | POST | 置顶歌曲 |
| 点歌 | `/api/room/{orderId}/queue/remove/{orderSongId}` | DELETE | 取消点歌 |
| 点歌 | `/api/room/{orderId}/queue` | GET | 已点列表 |
| 点歌 | `/api/room/{orderId}/played` | GET | 已唱列表 |
| 控制 | `/api/room/{orderId}/play/next` | POST | 切歌 |
| 控制 | `/api/room/{orderId}/play/replay` | POST | 重唱 |
| 控制 | `/api/room/{orderId}/play/pause` | POST | 暂停 |
| 控制 | `/api/room/{orderId}/play/resume` | POST | 继续播放 |
| 控制 | `/api/room/{orderId}/play/current` | GET | 当前播放状态 |

### 5.3 媒体流接口

| 模块 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 媒体流 | `/api/media/stream/{songId}` | GET | 流式传输音视频文件，支持 HTTP Range（断点续传/进度拖拽） |
| 媒体流 | `/api/media/cover/{songId}` | GET | 返回歌曲封面图片 |
| 上传（管理端） | `/api/admin/songs/{id}/upload` | POST | 上传本地音视频文件，存至服务器磁盘，记录 file_path |

---

## 六、非功能性需求

### 6.1 性能要求
- 歌曲搜索响应时间 ≤ 200ms（借助拼音索引 + Redis缓存）
- 点歌操作响应时间 ≤ 100ms（Redis队列操作）
- 系统支持同时 50 个包厢并发使用

### 6.2 安全要求
- 管理员密码 BCrypt 加密存储
- 后台接口基于 JWT Token 认证（前后端分离，Session不适用）
- Spring Boot 配置 CORS，允许前端域名跨域访问
- SQL注入防护（MyBatis参数化查询）
- XSS防护（输入过滤）
- React 前端本地存储 Token 至 localStorage，请求头携带 `Authorization: Bearer {token}`

### 6.3 数据要求
- 逻辑删除，不物理删除业务数据
- 订单数据永久保留
- 数据库字符集 `utf8mb4`，支持中文和特殊字符

---

## 七、开发里程碑

| 阶段 | 内容 | 预估时间 | 交付物 |
|------|------|----------|--------|
| M1 - 基础搭建 | 后端骨架、数据库、基础配置 | 2天 | 可运行的空项目 + 建表SQL |
| M2 - 后台管理后端 | 歌曲/歌手/分类/包厢CRUD API + JWT登录 | 4天 | 后台管理REST接口完整 |
| M3 - 后台管理前端 | admin-frontend React页面（登录+各管理页） | 3天 | 后台管理可视化操作 |
| M4 - 订单管理 | 开台/结账API + 前端页面 | 2天 | 订单流程闭环 |
| M5 - 点歌核心后端 | 歌曲检索 + 点歌排队 + 播放控制 API | 4天 | 包厢点歌REST接口完整 |
| M6 - 点歌前端 | room-frontend React点歌页面 | 3天 | 包厢端完整点歌体验 |
| M7 - Redis集成 | 热门排行、队列缓存、状态缓存 | 2天 | 性能优化完成 |
| M8 - 联调测试 | 前后端整体联调、Bug修复、测试数据 | 2天 | 可演示的完整系统 |
| **合计** | | **约22天** | |

---

## 八、风险评估

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| Jakarta EE 10 包名变更 | Spring Boot 3 将 `javax.*` 改为 `jakarta.*`，部分旧依赖不兼容 | 确认 MyBatis-Plus 3.5.7+ 版本，jjwt 使用 0.11.x+ |
| 前后端跨域 | React开发服务器与Spring Boot端口不同 | Vite配置代理，生产环境配置CORS |
| JWT Token管理 | Token过期导致用户体验差 | 设置合理过期时间（2小时），前端自动刷新 |
| 歌曲文件管理 | 大量文件存储 | 本期仅存路径，不做文件上传 |
| Redis单点 | 缓存不可用影响体验 | 做好降级策略，Redis挂了走DB |
| 拼音转换 | 多音字问题 | 使用 com.github.houbb:pinyin 库（pinyin4j已不维护，Spring Boot 3不友好） |
| **媒体文件格式兼容** | FLAC/MP4 浏览器支持度不一致 | FLAC 用 APlayer，MP4 视频用 react-player；后端返回正确 Content-Type |
| **大文件流式传输** | 直接读文件到内存会OOM | 使用 Spring `ResourceRegion` / `InputStreamResource` 配合 Range 请求，分块传输 |
| **文件路径配置** | 开发/生产环境路径不同 | application.yml 单独配置 `media.base-path`，不硬编码 |

---

## 九、约定与规范

### 9.1 编码规范
- 遵循阿里巴巴Java开发手册
- Controller层不写业务逻辑
- Service接口 + Impl实现分离
- 统一返回格式 `Result<T>` 包含 code/msg/data
- React 前端使用函数组件 + Hooks，禁止 Class 组件
- React 组件文件名 PascalCase，工具函数 camelCase

### 9.2 数据库规范
- 表名前缀 `t_`，字段名下划线命名
- 每表必有 `id`、`create_time`、`update_time`
- 业务表必有 `deleted` 逻辑删除字段
- 外键关系在应用层维护，不使用物理外键

### 9.3 接口规范
- RESTful风格
- 统一前缀 `/api/admin/`（后台）和 `/api/room/`（包厢端）
- 分页参数：`pageNum`（页码）、`pageSize`（每页条数）
- 统一异常处理，返回标准错误格式
- 前后端分离，登录认证改用 JWT，Token 在请求头 `Authorization: Bearer {token}` 传递
- 后端统一配置 CORS，开发阶段允许 `localhost:3000`、`localhost:3001`

---

_文档结束。此文档为开发任务分解的基础依据。_
