# KTV 点歌系统 - 协作指南

欢迎加入 KTV 点歌系统开发团队！本文档帮助你快速理解项目架构和开发规范。

## 快速导航

- [📚 项目概览](#项目概览)
- [🏗️ 技术架构](#技术架构)
- [📁 目录结构](#目录结构)
- [📖 文档索引](#文档索引)
- [🚀 常用命令](#常用命令)
- [❓ 常见问题](#常见问题)

---

## 📚 项目概览

### 项目简介
KTV 点歌管理系统是一个前后端分离的全栈应用，包含：
- **后台管理端**：歌曲、歌手、包厢、订单的 CRUD 管理
- **包厢点歌端**：用户点歌、排队、播放控制的触摸屏界面

### 核心特性
- ✅ 本地音视频文件流式播放（MP3/FLAC/MP4）
- ✅ Redis 缓存热门歌曲和排队队列
- ✅ 拼音快速检索
- ✅ JWT 无状态认证
- ✅ 支持点歌置顶、重唱、暂停等操作

### 技术栈
| 层级 | 技术 | 版本 |
|------|------|------|
| 后端 | Java | 21 |
| 框架 | Spring Boot | 3.2.x |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 6.x+ |
| 后台前端 | React + Ant Design | 18.x / 5.x |
| 包厢端前端 | React + Ant Design Mobile | 18.x / 5.x |
| 状态管理 | Zustand | 4.x |
| 构建 | Vite | 5.x |

---

## 🏗️ 技术架构

### 系统分层

```
┌─────────────────────────────────────────┐
│           前端层 (React + Vite)        │
│  admin-frontend (:3000)                 │
│  room-frontend (:3001)                  │
└────────────┬────────────────────────────┘
             │ HTTP/JSON (CORS)
             │ HTTP Range (媒体流)
┌────────────▼────────────────────────────┐
│      Spring Boot REST API (:8080)       │
│  Controller → Service → Mapper → DB     │
└────────────┬────────────────────────────┘
             │
┌────────────▼────────────────────────────┐
│  MySQL 8 (数据)  │  Redis (缓存/队列)   │
└─────────────────────────────────────────┘
```

---

## 📁 目录结构

### 后端结构 (ktv-backend/)

```
ktv-backend/
├── src/main/java/com/ktv/
│   ├── KtvApplication.java          # 启动类
│   ├── config/                      # 配置类
│   │   ├── CorsConfig.java          # 跨域配置
│   │   ├── MyBatisPlusConfig.java   # MyBatis-Plus 分页插件
│   │   ├── RedisConfig.java         # Redis 配置
│   │   └── WebMvcConfig.java        # MVC 配置
│   ├── controller/
│   │   ├── admin/                   # 后台管理接口 /api/admin/**
│   │   │   ├── AuthController.java  # 登录认证
│   │   │   ├── SongController.java # 歌曲管理
│   │   │   ├── SingerController.java
│   │   │   ├── CategoryController.java
│   │   │   ├── RoomController.java
│   │   │   └── OrderController.java
│   │   ├── room/                    # 包厢端接口 /api/room/**
│   │   │   ├── SongSearchController.java  # 歌曲检索
│   │   │   ├── PlayQueueController.java   # 点歌队列
│   │   │   ├── PlayControlController.java # 播放控制
│   │   │   └── HotSongController.java    # 热门排行
│   │   └── MediaStreamController.java    # 媒体流接口
│   ├── service/                     # 业务层
│   │   └── impl/                   # 实现类
│   ├── mapper/                      # 数据访问层
│   ├── entity/                      # 实体类
│   ├── dto/                         # 请求参数
│   ├── vo/                          # 响应参数
│   ├── common/                      # 公共类
│   │   ├── result/                  # Result 统一返回
│   │   ├── exception/               # 全局异常处理
│   │   └── util/                    # 工具类（JWT、拼音）
│   ├── interceptor/                 # 拦截器
│   └── task/                        # 定时任务
├── src/main/resources/
│   └── application.yml              # 配置文件
└── pom.xml                          # Maven 依赖
```

### 前端结构

#### admin-frontend/
```
admin-frontend/src/
├── api/                    # Axios 接口封装
│   ├── index.js           # 请求拦截器
│   └── *.js               # 各模块接口
├── pages/                 # 页面组件
│   ├── Login/             # 登录页
│   ├── Song/              # 歌曲管理
│   ├── Singer/            # 歌手管理
│   ├── Room/              # 包厢管理
│   └── Order/             # 订单管理
├── components/            # 公共组件
├── layouts/               # 布局组件
├── router/                # 路由配置
├── store/                 # Zustand 状态管理
├── utils/                 # 工具函数
├── App.jsx
└── main.jsx
```

#### room-frontend/
```
room-frontend/src/
├── api/                    # Axios 接口封装
├── pages/                 # 页面组件
│   ├── Join/              # 加入包厢页
│   ├── Search/            # 歌曲检索页
│   └── Queue/             # 已点/已唱列表
├── components/
│   ├── PlayBar.jsx        # 底部播放控制条（全局）
│   └── SongCard.jsx       # 歌曲卡片
├── store/
│   └── useRoomStore.js    # Zustand 状态管理
├── router/
└── main.jsx
```

---

## 📖 文档索引

### 快速上手
1. **[后端快速上手指南](backend-quickstart.md)** - 后端环境搭建、项目启动、开发流程
2. **[前端快速上手指南](frontend-quickstart.md)** - 前端环境搭建、项目启动、开发流程

### 开发文档
3. **[API 接口参考](api-reference.md)** - 所有 API 接口详细说明、请求参数、响应格式
4. **[数据库设计文档](database-schema.md)** - 数据库表结构、字段说明、索引设计

### 规范文档
5. **[编码规范](coding-standards.md)** - 命名规范、代码结构、注释规范、Git 提交规范

### 运维文档
6. **[部署指南](deployment-guide.md)** - 生产环境部署、Nginx 配置、HTTPS 配置、监控与备份

### 故障排查
7. **[常见问题排查指南](troubleshooting.md)** - 常见问题及解决方案、调试技巧

---

## 🚀 常用命令

### 后端

```bash
# 进入后端目录
cd ktv-backend

# 启动开发环境
mvn spring-boot:run

# 打包
mvn clean package

# 跳过测试打包
mvn clean package -DskipTests

# 运行测试
mvn test
```

### 前端（admin-frontend）

```bash
# 进入前台目录
cd admin-frontend

# 安装依赖
npm install

# 启动开发服务器（端口 3000）
npm run dev

# 构建生产版本
npm run build

# 预览构建结果
npm run preview
```

### 前端（room-frontend）

```bash
# 进入包厢端目录
cd room-frontend

# 安装依赖
npm install

# 启动开发服务器（端口 3001）
npm run dev

# 构建生产版本
npm run build
```

---

## ❓ 常见问题

### 1. CORS 跨域问题

**问题**：前端请求后端接口报错 `Access-Control-Allow-Origin`

**解决**：
- 开发环境：检查 `vite.config.js` 中的 `server.proxy` 配置
- 生产环境：检查后端 `CorsConfig.java` 是否允许前端域名

### 2. JWT Token 过期

**问题**：接口返回 `401 Unauthorized`

**解决**：
- 前端检查 `localStorage` 中是否有 `token`
- 后端检查 `JwtUtil.java` 中的过期时间设置

### 3. Redis 连接失败

**问题**：启动报错 `Unable to connect to Redis`

**解决**：
- 检查 Redis 服务是否启动
- 检查 `application.yml` 中的 Redis 配置（host、port）

### 4. 媒体文件播放失败

**问题**：音频/视频无法播放

**解决**：
- 检查文件路径 `media.base-path` 配置是否正确
- 检查文件是否存在
- 检查浏览器控制台的网络请求，确认 `Content-Type` 是否正确

### 5. 数据库连接失败

**问题**：启动报错 `Communications link failure`

**解决**：
- 检查 MySQL 服务是否启动
- 检查 `application.yml` 中的数据库配置（url、username、password）

更多问题请查看：[常见问题排查指南](troubleshooting.md)

---

## 相关文档

- [项目设计文档](../docs/project-overview.md)
- [开发任务列表](../docs/tasks/ktv-song-system-tasklist.md)
- [数据库初始化脚本](../sql/)

---

## 联系方式

- **作者**：shaun.sheng
- **项目路径**：`d:/个人/充电/练手项目/ktv`

---

祝你编码愉快！🎉
