# 数据库设计文档

本文档详细说明 KTV 点歌系统的数据库表结构和字段说明。

## 数据库信息

- **数据库名称**：`ktv_db`
- **字符集**：`utf8mb4`
- **排序规则**：`utf8mb4_unicode_ci`
- **数据库引擎**：`InnoDB`

---

## 表结构概览

```
t_singer          歌手表
t_category        歌曲分类表
t_song            歌曲表
t_room            包厢表
t_order           订单表
t_order_song      点歌记录表
t_sys_user        系统用户表
t_operation_log   操作日志表
```

---

## 表结构详情

### 1. 歌手表 (t_singer)

**说明**：存储歌手信息，包含拼音首字母用于快速检索

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
  INDEX `idx_name` (`name`),
  INDEX `idx_region` (`region`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌手表';
```

**字段说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 主键ID，自增 |
| name | VARCHAR(100) | 是 | 歌手名称 |
| pinyin | VARCHAR(200) | 否 | 拼音全拼，如 "zhoujielun" |
| pinyin_initial | VARCHAR(50) | 否 | 拼音首字母，如 "ZJL" |
| gender | TINYINT | 否 | 性别（0未知 1男 2女 3组合） |
| region | VARCHAR(50) | 否 | 地区（内地/港台/欧美/日韩/其他） |
| avatar | VARCHAR(500) | 否 | 头像URL |
| song_count | INT | 否 | 歌曲数量，冗余字段 |
| status | TINYINT | 否 | 状态（0禁用 1启用） |
| create_time | DATETIME | 否 | 创建时间，默认当前时间 |
| update_time | DATETIME | 否 | 更新时间，自动更新 |
| deleted | TINYINT | 否 | 逻辑删除（0未删 1已删） |

**索引说明**：
- `idx_pinyin_initial`：拼音首字母索引，加速拼音搜索
- `idx_name`：名称索引，加速歌手名搜索
- `idx_region`：地区索引，加速按地区筛选

---

### 2. 歌曲分类表 (t_category)

**说明**：存储歌曲分类信息

```sql
CREATE TABLE `t_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `sort_order` INT DEFAULT 0 COMMENT '排序序号',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌曲分类表';
```

**字段说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 主键ID，自增 |
| name | VARCHAR(50) | 是 | 分类名称（如：流行、经典、摇滚） |
| sort_order | INT | 否 | 排序序号，数字越小越靠前 |
| status | TINYINT | 否 | 状态（0禁用 1启用） |
| create_time | DATETIME | 否 | 创建时间 |
| update_time | DATETIME | 否 | 更新时间，自动更新 |

---

### 3. 歌曲表 (t_song)

**说明**：存储歌曲信息，包含文件路径、播放次数等

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
  INDEX `idx_name` (`name`),
  INDEX `idx_is_hot` (`is_hot`),
  INDEX `idx_is_new` (`is_new`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='歌曲表';
```

**字段说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 主键ID，自增 |
| name | VARCHAR(200) | 是 | 歌曲名称 |
| singer_id | BIGINT | 是 | 歌手ID，关联 t_singer.id |
| category_id | BIGINT | 否 | 分类ID，关联 t_category.id |
| pinyin | VARCHAR(500) | 否 | 拼音全拼 |
| pinyin_initial | VARCHAR(100) | 否 | 拼音首字母 |
| language | VARCHAR(20) | 否 | 语种（国语/粤语/英语/日语/韩语/其他） |
| duration | INT | 否 | 时长（秒） |
| file_path | VARCHAR(500) | 否 | 歌曲文件路径 |
| cover_url | VARCHAR(500) | 否 | 封面图片URL |
| play_count | INT | 否 | 总点播次数 |
| is_hot | TINYINT | 否 | 是否热门（0否 1是） |
| is_new | TINYINT | 否 | 是否新歌（0否 1是） |
| status | TINYINT | 否 | 状态（0下架 1上架） |
| create_time | DATETIME | 否 | 创建时间 |
| update_time | DATETIME | 否 | 更新时间，自动更新 |
| deleted | TINYINT | 否 | 逻辑删除（0未删 1已删） |

**索引说明**：
- `idx_singer_id`：歌手ID索引，加速按歌手查询
- `idx_category_id`：分类ID索引，加速按分类查询
- `idx_pinyin_initial`：拼音首字母索引，加速拼音搜索
- `idx_play_count`：播放次数降序索引，加速热门排行查询
- `idx_is_hot`：热门标识索引，加速热门歌曲查询
- `idx_is_new`：新歌标识索引，加速新歌查询

---

### 4. 包厢表 (t_room)

**说明**：存储包厢信息

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
  INDEX `idx_status` (`status`),
  INDEX `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='包厢表';
```

**字段说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 主键ID，自增 |
| name | VARCHAR(50) | 是 | 包厢名称（如：A01、豪华1号） |
| type | VARCHAR(20) | 是 | 类型（小包/中包/大包/豪华包） |
| capacity | INT | 否 | 容纳人数 |
| price_per_hour | DECIMAL(10,2) | 否 | 每小时价格 |
| status | TINYINT | 否 | 状态（0空闲 1使用中 2清洁中 3维修中） |
| description | VARCHAR(500) | 否 | 描述 |
| create_time | DATETIME | 否 | 创建时间 |
| update_time | DATETIME | 否 | 更新时间，自动更新 |
| deleted | TINYINT | 否 | 逻辑删除（0未删 1已删） |

**索引说明**：
- `idx_status`：状态索引，加速按状态筛选
- `idx_type`：类型索引，加速按类型筛选

---

### 5. 订单表 (t_order)

**说明**：存储包厢消费订单信息

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

**字段说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 主键ID，自增 |
| order_no | VARCHAR(50) | 是 | 订单编号，唯一 |
| room_id | BIGINT | 是 | 包厢ID，关联 t_room.id |
| start_time | DATETIME | 是 | 开台时间 |
| end_time | DATETIME | 否 | 结束时间 |
| duration_minutes | INT | 否 | 消费时长（分钟） |
| total_amount | DECIMAL(10,2) | 否 | 总费用 |
| status | TINYINT | 否 | 状态（1消费中 2已结账 3已取消） |
| remark | VARCHAR(500) | 否 | 备注 |
| operator_id | BIGINT | 否 | 操作员ID，关联 t_sys_user.id |
| create_time | DATETIME | 否 | 创建时间 |
| update_time | DATETIME | 否 | 更新时间，自动更新 |

**索引说明**：
- `uk_order_no`：订单号唯一索引
- `idx_room_id`：包厢ID索引，加速按包厢查询订单
- `idx_status`：状态索引，加速按状态筛选
- `idx_create_time`：创建时间索引，加速按时间查询

---

### 6. 点歌记录表 (t_order_song)

**说明**：存储点歌记录，关联订单和歌曲

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
  INDEX `idx_order_id_status` (`order_id`, `status`),
  INDEX `idx_song_id` (`song_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点歌记录表';
```

**字段说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 主键ID，自增 |
| order_id | BIGINT | 是 | 订单ID，关联 t_order.id |
| song_id | BIGINT | 是 | 歌曲ID，关联 t_song.id |
| song_name | VARCHAR(200) | 否 | 歌曲名（冗余） |
| singer_name | VARCHAR(100) | 否 | 歌手名（冗余） |
| sort_order | INT | 否 | 排序序号 |
| status | TINYINT | 否 | 状态（0等待 1播放中 2已播放 3已跳过） |
| play_time | DATETIME | 否 | 开始播放时间 |
| create_time | DATETIME | 否 | 点歌时间 |

**索引说明**：
- `idx_order_id`：订单ID索引，加速按订单查询
- `idx_order_id_status`：订单ID+状态复合索引，加速查询排队队列
- `idx_song_id`：歌曲ID索引，加速查询歌曲的点唱记录

---

### 7. 系统用户表 (t_sys_user)

**说明**：存储系统管理员账号信息

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

**字段说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 主键ID，自增 |
| username | VARCHAR(50) | 是 | 用户名，唯一 |
| password | VARCHAR(200) | 是 | 密码，BCrypt 加密存储 |
| real_name | VARCHAR(50) | 否 | 真实姓名 |
| role | VARCHAR(20) | 否 | 角色（super_admin/admin） |
| status | TINYINT | 否 | 状态（0禁用 1启用） |
| last_login_time | DATETIME | 否 | 最后登录时间 |
| create_time | DATETIME | 否 | 创建时间 |
| update_time | DATETIME | 否 | 更新时间，自动更新 |
| deleted | TINYINT | 否 | 逻辑删除（0未删 1已删） |

**索引说明**：
- `uk_username`：用户名唯一索引

---

### 8. 操作日志表 (t_operation_log)

**说明**：记录系统操作日志，用于审计

```sql
CREATE TABLE `t_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作员ID',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作员姓名',
  `module` VARCHAR(50) DEFAULT NULL COMMENT '操作模块',
  `operation` VARCHAR(50) DEFAULT NULL COMMENT '操作类型',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
  `params` TEXT DEFAULT NULL COMMENT '请求参数',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  INDEX `idx_operator_id` (`operator_id`),
  INDEX `idx_module` (`module`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';
```

**字段说明**：

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | BIGINT | 是 | 主键ID，自增 |
| operator_id | BIGINT | 否 | 操作员ID |
| operator_name | VARCHAR(50) | 否 | 操作员姓名 |
| module | VARCHAR(50) | 否 | 操作模块（如：歌曲管理） |
| operation | VARCHAR(50) | 否 | 操作类型（如：新增、修改、删除） |
| description | VARCHAR(500) | 否 | 操作描述 |
| params | TEXT | 否 | 请求参数（JSON） |
| ip | VARCHAR(50) | 否 | IP地址 |
| create_time | DATETIME | 否 | 操作时间 |

**索引说明**：
- `idx_operator_id`：操作员ID索引
- `idx_module`：模块索引
- `idx_create_time`：操作时间索引

---

## ER 关系图

```
[t_singer 歌手] 1 ─── N [t_song 歌曲] N ─── 1 [t_category 分类]
                        │
                        N
                        │
[t_room 包厢] 1 ─── N [t_order 订单] 1 ─── N [t_order_song 点歌记录]
                        │                                  │
                        N                                  N
                        │                                  │
                    [t_sys_user 用户]                   [t_song 歌曲]

[t_sys_user 用户] ─── [t_operation_log 操作日志]
```

---

## Redis 数据结构

| Key 格式 | 类型 | 说明 | 过期时间 |
|---------|------|------|----------|
| `ktv:queue:{orderId}` | List | 包厢点歌排队队列，存储 orderSong 的 ID 列表 | 随订单结束清除 |
| `ktv:playing:{orderId}` | String | 当前正在播放的歌曲 orderSongId | 随订单结束清除 |
| `ktv:play:status:{orderId}` | String | 播放状态（playing/paused） | 随订单结束清除 |
| `ktv:song:hot` | ZSet | 热门歌曲排行榜，score=点播次数 | 24小时 |
| `ktv:song:cache:{songId}` | Hash | 歌曲详情缓存 | 1小时 |

---

## 数据初始化

### 默认管理员账号

```sql
-- 用户名：admin
-- 密码：admin123（BCrypt 加密）
INSERT INTO t_sys_user (username, password, real_name, role) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EHs', '超级管理员', 'super_admin');
```

### 初始数据

- 8 个歌曲分类（流行、经典、摇滚、民谣、电子、R&B、说唱、儿歌）
- 23 位歌手（内地/港台/欧美/日韩）
- 64 首歌曲（周杰伦、林俊杰、邓紫棋、五月天、Taylor Swift 等热门歌曲）
- 10 个包厢（小包3个、中包3个、大包2个、豪华包2个）

详细数据请查看 `sql/init-data.sql` 文件。

---

## 相关文档

- [项目设计文档](../docs/project-overview.md)
- [开发任务列表](../docs/tasks/ktv-song-system-tasklist.md)
- [API 接口参考](api-reference.md)

---

**作者**：shaun.sheng

祝你开发愉快！🗄️
