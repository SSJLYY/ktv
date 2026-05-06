# 项目总览与快速上手

本文档是 KTV 点歌系统的总览型 skill，适合第一次接手项目的人快速建立全局认知。它不替代细分文档，而是负责说明项目边界、启动顺序、关键约定与阅读路径。

## 1. 这是什么项目

KTV 点歌系统是一个前后端分离的全栈项目，包含两类前端与一个后端：

- **后台管理端**：面向店员或管理员，负责歌曲、歌手、分类、包厢、订单、媒体资源等管理
- **包厢点歌端**：面向顾客，负责检索歌曲、点歌排队、播放控制、查看已点/已唱列表
- **后端服务**：统一提供管理端接口、包厢端接口、媒体流接口、缓存与队列能力

### 当前阶段
- M1-M9 已完成
- **M10 联调测试待进行**

## 2. 系统组成一图看懂

- `ktv-backend`：Spring Boot 后端，默认端口 `8080`
- `admin-frontend`：后台管理前端，默认端口 `3000`
- `room-frontend`：包厢点歌前端，默认端口 `3001`
- `sql`：数据库初始化脚本
- `skills`：项目协作与上手文档
- MySQL：主业务数据
- Redis：热门排行、点歌队列、播放状态等缓存/队列数据
- 媒体目录：存放歌曲音视频文件与封面资源

## 3. 技术栈与真实版本

> **重要：版本与配置冲突时，一律以代码和配置文件为准。**

### 后端
- Spring Boot `3.2.5`
- MyBatis-Plus `3.5.7`
- MySQL 8
- Redis 6+
- JWT 认证
- TinyPinyin `2.0.3.RELEASE`

### 前端
- React `19.2.4`
- Vite `8.0.1`
- admin 端：Ant Design 5
- room 端：Ant Design Mobile 5 + APlayer + ReactPlayer
- Zustand 5
- Axios 1

### 需要特别注意的真实版本
- `ktv-backend/pom.xml` 中的 **Java 版本是 17**，不是根 README 里写的 21
- `admin-frontend/package.json` 和 `room-frontend/package.json` 中的 **React 版本是 19.2.4**，不是根 README 里写的 18

## 4. 3 分钟快速上手

### 环境依赖
- JDK 17+
- Maven 3.6.3+
- Node.js 18+
- MySQL 8+
- Redis 6+

### 数据库初始化
> **以 `sql/init-schema.sql` 和 `ktv-backend/src/main/resources/application.yml` 为准。**

当前实际数据库名：`ktv_db`

```bash
mysql -u root -p < sql/init-schema.sql
mysql -u root -p ktv_db < sql/init-data.sql
```

### 启动顺序
1. 启动 MySQL
2. 启动 Redis
3. 启动后端 `ktv-backend`
4. 启动 `admin-frontend`
5. 启动 `room-frontend`

### 常用命令

#### 后端
```bash
cd d:/个人/充电/练手项目/ktv/ktv-backend
mvn spring-boot:run
mvn test
mvn clean package
```

#### 后台管理端
```bash
cd d:/个人/充电/练手项目/ktv/admin-frontend
npm install
npm run dev
npm run lint
npm run build
```

#### 包厢点歌端
```bash
cd d:/个人/充电/练手项目/ktv/room-frontend
npm install
npm run dev
npm run lint
npm run build
```

### 访问地址
- 后端：`http://localhost:8080`
- admin：`http://localhost:3000`
- room：`http://localhost:3001`
- admin 默认账号：`admin / admin123`

## 5. 关键开发约定

这些内容是高频常识，接手开发时必须先记住。

### 接口分流
- 管理端接口前缀：`/api/admin/`
- 包厢端接口前缀：`/api/room/`
- 媒体接口前缀：`/api/media/`

### 认证边界
- `/api/admin/**`：**需要 JWT**，请求头使用 `Authorization: Bearer {token}`
- `/api/room/**`：**不需要 JWT**，主要通过 `orderId` 标识会话

### 数据库与后端约定
- 表名前缀统一为：`t_`
- 字段命名统一为：下划线风格（snake_case）
- 逻辑删除字段统一为：`deleted`（0 未删，1 已删）
- 通用时间字段：`create_time`、`update_time`
- 分页参数使用：`current/size`，**不是** `pageNum/pageSize`
- MyBatis-Plus 已配置逻辑删除与下划线转驼峰

### 前端约定
- 统一使用函数组件 + Hooks
- 路由采用懒加载
- admin 端 token 存在 `localStorage`
- Zustand 用于状态管理，支持持久化
- room 端 `orderId` 使用 Zustand persist 持久化

### Redis 关键约定
- `ktv:queue:{orderId}`：点歌队列
- `ktv:playing:{orderId}`：当前播放歌曲 ID
- `ktv:play:status:{orderId}`：播放状态
- `ktv:current_order:room:{roomId}`：包厢当前订单
- `ktv:song:hot`：热门排行

### 作者约定
- 所有作者统一为：`shaun.sheng`

## 6. 代码入口导航

### 后端入口
- 启动类：`ktv-backend/src/main/java/com/ktv/KtvApplication.java`
- 管理端控制器：`ktv-backend/src/main/java/com/ktv/controller/admin/`
- 包厢端控制器：`ktv-backend/src/main/java/com/ktv/controller/room/`
- 配置类：`ktv-backend/src/main/java/com/ktv/config/`
- 业务层：`ktv-backend/src/main/java/com/ktv/service/` 与 `service/impl/`
- 数据访问层：`ktv-backend/src/main/java/com/ktv/mapper/`
- 实体/DTO/VO：`entity/`、`dto/`、`vo/`
- 配置文件：`ktv-backend/src/main/resources/application.yml`

### admin-frontend 入口
- 接口封装：`admin-frontend/src/api/`
- 页面：`admin-frontend/src/pages/`
- 路由：`admin-frontend/src/router/`
- 状态：`admin-frontend/src/store/`
- 布局：`admin-frontend/src/layouts/`

### room-frontend 入口
- 接口封装：`room-frontend/src/api/`
- 页面：`room-frontend/src/pages/`
- 组件：`room-frontend/src/components/`
- 路由：`room-frontend/src/router/`
- 状态：`room-frontend/src/store/`

## 7. 配置差异与判定原则

这个项目存在一些文档与实现不完全一致的地方，处理原则如下：

1. **版本差异看 `pom.xml` 和 `package.json`**
2. **数据库名看 `sql/init-schema.sql` 与 `application.yml`**
3. **媒体目录看 `application.yml`**
4. **接口边界看 controller 包结构与实际路由**

### 目前已确认的差异
- Java：README 写 21，实际 `pom.xml` 为 17
- React：README 写 18，实际两个前端都是 19.2.4
- 数据库：README 示例有 `ktv`，实际脚本和配置都使用 `ktv_db`
- 媒体目录：`application.yml` 默认值是 `/data/ktv-media`；如果是本地 Windows 开发，可按实际环境改成 `D:/ktv-media`

## 8. 推荐阅读顺序

如果你是第一次接手，按下面顺序读：

1. **当前文档**：先建立项目全局认知
2. [后端快速上手指南](backend-quickstart.md)
3. [前端快速上手指南](frontend-quickstart.md)
4. [API 接口参考](api-reference.md)
5. [数据库设计文档](database-schema.md)
6. [编码规范](coding-standards.md)
7. [部署指南](deployment-guide.md)
8. [常见问题排查指南](troubleshooting.md)

## 9. 适用方式

这个文档适合下面几种场景：

- 新人第一次接手项目
- AI 助手首次接入项目上下文
- 代码评审前建立共同背景
- 联调前统一环境与关键约定

如果你已经明确要改某一层：
- 查接口细节：看 `api-reference.md`
- 查库表和 Redis：看 `database-schema.md`
- 查启动与环境：看 `backend-quickstart.md`、`frontend-quickstart.md`
- 查规范：看 `coding-standards.md`
- 查部署：看 `deployment-guide.md`
- 查报错与排障：看 `troubleshooting.md`
