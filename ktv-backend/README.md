# KTV点歌系统 - 后端服务

## 项目简介

KTV点歌管理系统后端服务，基于Spring Boot 3.2.x + MyBatis-Plus + MySQL + Redis开发。

## 技术栈

- **JDK**: 21 (LTS)
- **框架**: Spring Boot 3.2.5
- **ORM**: MyBatis-Plus 3.5.7
- **数据库**: MySQL 8.0
- **缓存**: Redis 6.x+
- **认证**: JWT (jjwt 0.12.6)
- **工具**: Lombok、pinyin

## 项目结构

```
ktv-backend/
├── src/main/java/com/ktv/
│   ├── config/              # 配置类（CORS、MyBatis-Plus、Redis等）
│   ├── controller/          # 控制器层
│   │   ├── admin/           # 后台管理接口
│   │   └── room/            # 包厢点歌接口
│   ├── service/             # 业务逻辑层
│   │   └── impl/            # 业务实现
│   ├── mapper/              # 数据访问层
│   ├── entity/              # 实体类
│   ├── dto/                 # 请求参数对象
│   ├── vo/                  # 响应视图对象
│   └── common/              # 公共类（Result、异常、工具类等）
│       ├── result/          # 统一返回结果
│       ├── exception/       # 异常处理
│       └── util/            # 工具类
├── src/main/resources/
│   ├── application.yml      # 配置文件
│   └── mapper/              # MyBatis XML映射文件
└── pom.xml                  # Maven配置
```

## 快速开始

### 1. 环境要求

- JDK 21+
- Maven 3.6.3+
- MySQL 8.0+
- Redis 6.x+

### 2. 数据库准备

创建数据库并执行初始化脚本：

```sql
CREATE DATABASE ktv CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE ktv;

-- 执行 sql/init-schema.sql 建表
-- 执行 sql/init-data.sql 插入测试数据
```

### 3. Redis准备

启动Redis服务：

```bash
redis-server
```

### 4. 修改配置

编辑 `src/main/resources/application.yml`，根据实际情况修改配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ktv?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password

  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password

media:
  base-path: D:/ktv-media
```

### 5. 启动项目

```bash
# 编译项目
mvn clean install

# 启动项目
mvn spring-boot:run
```

或者直接在IDE中运行 `KtvApplication` 类的 `main` 方法。

### 6. 验证启动

访问健康检查接口：

```bash
curl http://localhost:8080/health
```

返回示例：

```json
{
  "status": "UP",
  "service": "KTV Backend API",
  "version": "1.0.0",
  "timestamp": "2026-03-30T17:48:00",
  "message": "系统运行正常"
}
```

## API接口文档

### 后台管理接口

- `POST /api/admin/login` - 管理员登录
- `POST /api/admin/logout` - 退出登录
- `GET/POST/PUT/DELETE /api/admin/songs` - 歌曲管理
- `GET/POST/PUT/DELETE /api/admin/singers` - 歌手管理
- `GET/POST/PUT/DELETE /api/admin/categories` - 分类管理
- `GET/POST/PUT/DELETE /api/admin/rooms` - 包厢管理
- `GET/POST /api/admin/orders` - 订单管理

### 包厢点歌接口

- `GET /api/room/songs/search` - 歌曲搜索
- `GET /api/room/songs/by-singer/{singerId}` - 按歌手查歌
- `GET /api/room/songs/by-category/{categoryId}` - 按分类查歌
- `GET /api/room/songs/hot` - 热门排行
- `POST /api/room/{orderId}/queue/add` - 点歌
- `POST /api/room/{orderId}/queue/top/{orderSongId}` - 置顶
- `DELETE /api/room/{orderId}/queue/remove/{orderSongId}` - 取消点歌
- `GET /api/room/{orderId}/queue` - 已点列表
- `GET /api/room/{orderId}/played` - 已唱列表
- `POST /api/room/{orderId}/play/next` - 切歌
- `POST /api/room/{orderId}/play/replay` - 重唱
- `POST /api/room/{orderId}/play/pause` - 暂停
- `POST /api/room/{orderId}/play/resume` - 继续播放
- `GET /api/room/{orderId}/play/current` - 当前播放状态

### 媒体流接口

- `GET /api/media/stream/{songId}` - 流式传输音视频文件
- `GET /api/media/cover/{songId}` - 获取歌曲封面

## 注意事项

1. **Spring Boot 3 使用 jakarta.* 包名**（不再是 javax.*），注意引入的依赖版本
2. **MySQL驱动已改名为 mysql-connector-j**（不是 mysql-connector-java）
3. **前后端分离架构**，使用JWT Token认证，不支持Session
4. **CORS已配置**，允许 localhost:3000 和 localhost:3001 访问
5. **媒体文件路径**需要提前配置好，确保有写权限

## 开发规范

- 统一返回格式：`Result<T>` (code/message/data)
- 分页参数：`pageNum` / `pageSize`
- 逻辑删除：`deleted` 字段（0未删除，1已删除）
- 字段命名：数据库使用下划线，Java使用驼峰
- 接口前缀：
  - 后台管理：`/api/admin/**`
  - 包厢端：`/api/room/**`
  - 媒体流：`/api/media/**`

## 作者

KTV Team

## 版本

v1.0.0 (2026-03-30)
