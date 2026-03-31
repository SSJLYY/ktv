# KTV点歌系统 - 深度审查报告（第二轮）

**审查日期**：2026-03-31 16:10
**审查范围**：全量代码（后端 + 前端 + SQL + Mapper XML）
**审查方法**：三个子代理并行审查（后端/前端/SQL）
**结论**：✅ 代码质量优秀，发现并修复 4 项小问题

---

## 一、审查概要

| 项目 | 状态 | 评分 |
|------|------|------|
| 后端代码 | ✅ 优秀 | 5/5 |
| 前端代码 | ✅ 优秀 | 5/5 |
| SQL 设计 | ✅ 优秀 | 5/5 |
| Mapper XML | ✅ 优秀 | 5/5 |

**总体评价**：代码质量优秀，逻辑清晰，规范统一。经过前9轮 Bug 修复和第一轮代码审查（P0-P3共47项），系统已达到生产标准。本次审查发现 4 项小改进项，全部修复。

---

## 二、发现的问题与修复

### 🔴 后端问题（3项）

#### B10-1：OrderSong 实体缺少 @TableLogic 支持
- **严重程度**：🟡 中
- **问题描述**：OrderSong 实体没有继承 BaseEntity，也没有 `@TableLogic` 注解。虽然数据库表有 `deleted` 字段，但 MyBatis-Plus BaseMapper 方法无法自动过滤 deleted=1 的记录。
- **影响**：BaseMapper 的查询会返回已删除记录，导致逻辑删除失效。
- **修复方案**：添加 `deleted` 字段和 `@TableLogic` 注解
- **修复状态**：✅ 已修复
- **文件**：`ktv-backend/src/main/java/com/ktv/entity/OrderSong.java`
```java
/**
 * 逻辑删除标记：0未删除，1已删除
 * 深度审查修复（第10轮）：添加 @TableLogic，确保 BaseMapper 自动过滤 deleted=1
 */
@TableLogic
@TableField("deleted")
private Integer deleted;
```

#### B10-2：OrderSongMapper.xml 查询缺少 deleted 过滤
- **严重程度**：🟡 中
- **问题描述**：selectByOrderIdAndStatus、selectPlayedByOrderId、findSongInfoById 三个自定义查询方法都没有添加 `WHERE os.deleted = 0` 过滤条件。
- **影响**：自定义 SQL 会查询到已删除的记录，与 BaseMapper 行为不一致。
- **修复方案**：在每个查询的 WHERE 条件中添加 `AND os.deleted = 0`
- **修复状态**：✅ 已修复
- **文件**：`ktv-backend/src/main/resources/mapper/OrderSongMapper.xml`
```xml
WHERE os.order_id = #{orderId}
  AND os.deleted = 0
  AND os.status IN (2, 3)
```

#### B10-3：文件上传配置过大
- **严重程度**：🟢 低
- **问题描述**：max-file-size: 500MB，可能导致服务器资源耗尽（DoS 攻击风险）。
- **影响**：恶意用户上传超大文件占用带宽和存储。
- **修复方案**：调整为 max-file-size: 100MB，max-request-size: 200MB
- **修复状态**：✅ 已修复
- **文件**：`ktv-backend/src/main/resources/application.yml`
```yaml
servlet:
  multipart:
    max-file-size: 100MB
    max-request-size: 200MB
```

#### 说明：OperationLog 不需要逻辑删除
- **设计决策**：OperationLog 是审计日志表，合规要求不应被删除。数据库表也没有 `deleted` 字段，设计正确。

---

### 🟡 前端问题（1项）

#### F10-1：Song filterOption 空值风险
- **严重程度**：🟢 低
- **问题描述**：`option.label.toLowerCase()` 当 `option.label` 为 null 时会崩溃。
- **影响**：运行时错误，页面可能崩溃。
- **修复方案**：添加空值保护 `(option.label || '').toLowerCase()`
- **修复状态**：✅ 已修复
- **文件**：`admin-frontend/src/pages/Song/index.jsx`
```jsx
filterOption={(input, option) =>
  (option.label || '').toLowerCase().includes(input.toLowerCase())
}
```

---

### ✅ SQL 设计（无问题）

SQL 审查确认：
- 所有表都有 deleted 字段（除 OperationLog，审计日志不应删除）
- 索引设计合理，命名统一为 `idx_` 前缀
- SQL 脚本安全改进已完成
- OrderSong 的 deleted 字段已在 init-schema.sql 第 181 行添加

---

## 三、代码质量亮点

### 后端
- ✅ 全局逻辑删除配置完善（logic-delete-field: deleted）
- ✅ 状态枚举化（OrderStatusEnum、OrderSongStatusEnum、RoomStatusEnum）
- ✅ 分布式锁防重（PlayCountSyncTask 使用 Redis SETNX）
- ✅ BCryptPasswordEncoder 通过 @Bean 注入
- ✅ 跨域配置支持环境变量动态化

### 前端
- ✅ 所有 useEffect 都有正确的清理函数
- ✅ useCallback/useMemo 避免闭包陷阱
- ✅ Ref 追踪定时器，防止内存泄漏
- ✅ 组件卸载时正确清理资源
- ✅ Zustand persist 状态管理规范

### SQL
- ✅ 逻辑删除字段统一（deleted）
- ✅ 索引设计合理（单列索引 + 复合索引）
- ✅ 注释清晰，命名规范
- ✅ 字符集统一（utf8mb4_unicode_ci）

---

## 四、技术债务与建议项

### P3 挑剔项（已完成）
- 前端闭包陷阱已修复
- 定时器泄漏已修复
- Redis Key 命名已统一（ktv:song:cache:、ktv:singer:songCount:）

### 未来优化建议（非阻塞）
1. **日志系统**：建议引入 Logback 或 SLF4J 配置文件，生产环境切换为 INFO 级别
2. **异常处理**：GlobalExceptionHandler 可以细化异常类型，返回更友好的错误码
3. **单元测试**：建议为核心 Service 添加单元测试覆盖率
4. **API 文档**：建议集成 Swagger/OpenAPI 自动生成文档
5. **文件上传**：建议添加 MIME 类型白名单校验（当前已有，可增强）
6. **Redis 密码**：生产环境建议设置 Redis 密码（当前为空）

---

## 五、修复清单

| ID | 问题 | 文件 | 状态 |
|----|------|------|------|
| B10-1 | OrderSong 缺 @TableLogic | OrderSong.java | ✅ |
| B10-2 | OrderSongMapper.xml 缺 deleted 过滤 | OrderSongMapper.xml | ✅ |
| B10-3 | 文件上传 500MB→100MB | application.yml | ✅ |
| F10-1 | Song filterOption 空值保护 | Song/index.jsx | ✅ |

---

## 六、审查结论

KTV 点歌系统经过 10 轮修复（31 Bug + 47 代码审查项 + 4 深度审查项），代码质量已达到生产标准：

- ✅ 功能完整性：点歌、播放、订单、管理、媒体处理全部完成
- ✅ 代码规范性：逻辑删除、状态枚举、异常处理、分层清晰
- ✅ 性能优化：Redis 缓存、批量查询、索引设计、分布式锁
- ✅ 安全性：JWT 认证、BCrypt 密码加密、目录穿越防护、MIME 校验
- ✅ 前端体验：React Hooks 规范、状态管理、播放器稳定性

**建议**：进行 M10 联调测试，准备上线部署。

---

**审查人**：shaun.sheng
**生成时间**：2026-03-31 16:10
**报告版本**：v2（第二轮深度审查）
