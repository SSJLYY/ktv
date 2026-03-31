# 后端快速上手指南

本文档帮助你快速上手 KTV 点歌系统的后端开发。

## 环境准备

### 必需软件

| 软件 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 21+ | 必须是 JDK 21，Spring Boot 3 不兼容旧版本 |
| Maven | 3.6.3+ | 用于构建项目 |
| MySQL | 8.0+ | 数据库服务 |
| Redis | 6.x+ | 缓存服务 |
| IDE | IntelliJ IDEA 2023+ | 推荐 |

### 安装检查

```bash
# 检查 JDK 版本
java -version

# 检查 Maven 版本
mvn -version

# 检查 MySQL
mysql --version

# 检查 Redis
redis-cli --version
```

---

## 项目启动

### 1. 克隆项目

```bash
cd d:/个人/充电/练手项目/ktv/ktv-backend
```

### 2. 配置数据库

#### 2.1 创建数据库

```sql
CREATE DATABASE ktv_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

#### 2.2 执行初始化脚本

```bash
# 进入 SQL 目录
cd ../sql

# 执行建表脚本
mysql -u root -p ktv_db < init-schema.sql

# 执行数据初始化脚本
mysql -u root -p ktv_db < init-data.sql
```

### 3. 配置 application.yml

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ktv_db?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password  # 修改为你的密码
  data:
    redis:
      host: localhost
      port: 6379
      database: 0

media:
  base-path: D:/ktv-media  # 修改为你的媒体文件存储路径
```

### 4. 启动 Redis

```bash
# Windows
redis-server.exe

# 或启动 Redis 服务
net start redis
```

### 5. 启动项目

```bash
# 进入后端目录
cd ktv-backend

# Maven 启动
mvn spring-boot:run

# 或在 IDE 中直接运行 KtvApplication.java
```

### 6. 验证启动

访问健康检查接口：
```
http://localhost:8080/health
```

期望返回：
```json
{
  "code": 200,
  "msg": "success",
  "data": "KTV系统运行正常"
}
```

---

## 开发流程

### 添加新接口（示例：添加"歌单管理"接口）

#### 1. 创建 Entity

```java
package com.ktv.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("t_playlist")
public class Playlist extends BaseEntity {
    private String name;
    private String description;
    private Integer songCount;
    private Integer status; // 0禁用 1启用
}
```

#### 2. 创建 Mapper

```java
package com.ktv.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ktv.entity.Playlist;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlaylistMapper extends BaseMapper<Playlist> {
}
```

#### 3. 创建 Service

```java
package com.ktv.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ktv.entity.Playlist;

public interface PlaylistService extends IService<Playlist> {
}
```

```java
package com.ktv.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ktv.entity.Playlist;
import com.ktv.mapper.PlaylistMapper;
import com.ktv.service.PlaylistService;
import org.springframework.stereotype.Service;

@Service
public class PlaylistServiceImpl extends ServiceImpl<PlaylistMapper, Playlist>
    implements PlaylistService {
}
```

#### 4. 创建 Controller

```java
package com.ktv.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.common.result.Result;
import com.ktv.entity.Playlist;
import com.ktv.service.PlaylistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/playlists")
public class PlaylistController {

    @Autowired
    private PlaylistService playlistService;

    @GetMapping
    public Result<Page<Playlist>> list(
        @RequestParam(defaultValue = "1") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Page<Playlist> page = playlistService.page(new Page<>(pageNum, pageSize));
        return Result.success(page);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Playlist playlist) {
        playlistService.save(playlist);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Playlist playlist) {
        playlist.setId(id);
        playlistService.updateById(playlist);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        playlistService.removeById(id);
        return Result.success();
    }
}
```

#### 5. 测试接口

```bash
# 查询列表
curl http://localhost:8080/api/admin/playlists

# 新增
curl -X POST http://localhost:8080/api/admin/playlists \
  -H "Content-Type: application/json" \
  -d '{"name":"经典老歌","description":"80-90年代经典歌曲"}'

# 修改
curl -X PUT http://localhost:8080/api/admin/playlists/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"经典老歌精选","description":"精选80-90年代经典歌曲"}'

# 删除
curl -X DELETE http://localhost:8080/api/admin/playlists/1
```

---

## 常用代码片段

### 分页查询

```java
@GetMapping
public Result<Page<Song>> list(
    @RequestParam(defaultValue = "1") Integer pageNum,
    @RequestParam(defaultValue = "10") Integer pageSize,
    @RequestParam(required = false) String keyword
) {
    // 构建查询条件
    LambdaQueryWrapper<Song> wrapper = new LambdaQueryWrapper<>();
    if (StringUtils.isNotBlank(keyword)) {
        wrapper.like(Song::getName, keyword)
               .or()
               .like(Song::getPinyinInitial, keyword);
    }
    wrapper.orderByDesc(Song::getCreateTime);

    // 分页查询
    Page<Song> page = songService.page(new Page<>(pageNum, pageSize), wrapper);
    return Result.success(page);
}
```

### Redis 操作

```java
@Autowired
private StringRedisTemplate redisTemplate;

// 设置值
redisTemplate.opsForValue().set("ktv:key", "value", 1, TimeUnit.HOURS);

// 获取值
String value = redisTemplate.opsForValue().get("ktv:key");

// 列表操作
redisTemplate.opsForList().rightPush("ktv:queue:1", "songId");
String songId = redisTemplate.opsForList().leftPop("ktv:queue:1");

// ZSet 操作（热门排行）
redisTemplate.opsForZSet().incrementScore("ktv:song:hot", "songId", 1);
Set<String> hotSongs = redisTemplate.opsForZSet().reverseRange("ktv:song:hot", 0, 9);
```

### 事务处理

```java
@Transactional(rollbackFor = Exception.class)
public void openOrder(Long roomId, Long operatorId) {
    // 1. 更新包厢状态
    Room room = roomService.getById(roomId);
    room.setStatus(1); // 使用中
    roomService.updateById(room);

    // 2. 创建订单
    Order order = new Order();
    order.setRoomId(roomId);
    order.setOrderNo(OrderNoUtil.generate());
    order.setStartTime(LocalDateTime.now());
    order.setStatus(1); // 消费中
    order.setOperatorId(operatorId);
    orderService.save(order);

    // 3. 记录操作日志
    // ...
}
```

---

## 调试技巧

### 1. 日志输出

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class TestController {
    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    @GetMapping("/test")
    public Result<Void> test() {
        log.info("这是一条 INFO 日志");
        log.debug("这是一条 DEBUG 日志");
        log.error("这是一条 ERROR 日志");
        return Result.success();
    }
}
```

### 2. 查看 SQL 日志

在 `application.yml` 中配置：

```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 3. 断点调试

在 IDE 中：
1. 在代码行号左侧点击，设置断点
2. 以 Debug 模式启动 `KtvApplication.java`
3. 访问接口，触发断点
4. 使用调试工具栏（Step Over、Step Into 等）查看变量值

---

## 常见错误

### 1. `java.lang.NoClassDefFoundError: jakarta/...`

**原因**：依赖版本不兼容，Spring Boot 3 使用 `jakarta.*` 包名

**解决**：
- 检查 MyBatis-Plus 版本是否 >= 3.5.7
- 检查 jjwt 版本是否 >= 0.11.5

### 2. `Communications link failure`

**原因**：数据库连接失败

**解决**：
- 检查 MySQL 服务是否启动
- 检查 `application.yml` 中的数据库配置

### 3. `Unable to connect to Redis`

**原因**：Redis 连接失败

**解决**：
- 检查 Redis 服务是否启动
- 检查 Redis 配置（host、port）

---

## 下一步

- 阅读 [项目设计文档](../docs/project-overview.md)
- 查看 [开发任务列表](../docs/tasks/ktv-song-system-tasklist.md)
- 了解 [接口设计](../docs/project-overview.md#五接口设计概要)

---

**作者**：shaun.sheng

祝你编码愉快！🚀
