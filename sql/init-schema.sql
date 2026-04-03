-- ========================================================
-- KTV点歌系统 - 数据库初始化脚本
-- 版本: v1.0
-- 创建日期: 2026-03-30
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- ========================================================

-- 创建数据库（如不存在）
CREATE DATABASE IF NOT EXISTS ktv_db 
    DEFAULT CHARACTER SET utf8mb4 
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ktv_db;

-- ========================================================
-- 1. 歌手表 t_singer
-- ========================================================
DROP TABLE IF EXISTS `t_singer`;

CREATE TABLE `t_singer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '歌手名',
    `pinyin` VARCHAR(200) DEFAULT NULL COMMENT '拼音全拼',
    `pinyin_initial` VARCHAR(50) DEFAULT NULL COMMENT '拼音首字母（大写）',
    `gender` TINYINT DEFAULT 0 COMMENT '性别：0未知 1男 2女 3组合',
    `region` VARCHAR(50) DEFAULT NULL COMMENT '地区：内地/港台/欧美/日韩/其他',
    `avatar` VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `song_count` INT DEFAULT 0 COMMENT '歌曲数量（冗余字段）',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    -- M17修复：添加唯一约束，防止同一地区插入同名歌手
    UNIQUE KEY `uk_name_region` (`name`, `region`),
    KEY `idx_pinyin_initial` (`pinyin_initial`),
    KEY `idx_region` (`region`),
    KEY `idx_status_deleted` (`status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='歌手表';


-- ========================================================
-- 2. 歌曲分类表 t_category
-- ========================================================
DROP TABLE IF EXISTS `t_category`;

CREATE TABLE `t_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号（越小越靠前）',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    -- M18修复：添加唯一约束，防止重复分类名
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_sort_order` (`sort_order`),
    KEY `idx_status_deleted` (`status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='歌曲分类表';


-- ========================================================
-- 3. 歌曲表 t_song
-- ========================================================
DROP TABLE IF EXISTS `t_song`;

CREATE TABLE `t_song` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(200) NOT NULL COMMENT '歌曲名',
    `singer_id` BIGINT NOT NULL COMMENT '歌手ID',
    `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
    `pinyin` VARCHAR(500) DEFAULT NULL COMMENT '拼音全拼',
    `pinyin_initial` VARCHAR(100) DEFAULT NULL COMMENT '拼音首字母（大写）',
    `language` VARCHAR(20) DEFAULT '国语' COMMENT '语种：国语/粤语/英语/日语/韩语/其他',
    `duration` INT DEFAULT 0 COMMENT '时长（秒）',
    `file_path` VARCHAR(500) DEFAULT NULL COMMENT '歌曲文件相对路径（相对于media.base-path）',
    `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
    `lyric_path` VARCHAR(500) DEFAULT NULL COMMENT '歌词文件路径',
    `play_count` INT DEFAULT 0 COMMENT '总点播次数',
    `is_hot` TINYINT DEFAULT 0 COMMENT '是否热门：0否 1是',
    `is_new` TINYINT DEFAULT 0 COMMENT '是否新歌：0否 1是',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0下架 1上架',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_singer_id` (`singer_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_pinyin_initial` (`pinyin_initial`),
    KEY `idx_language` (`language`),
    KEY `idx_play_count` (`play_count` DESC),
    KEY `idx_is_hot` (`is_hot`),
    KEY `idx_is_new` (`is_new`),
    KEY `idx_status_deleted` (`status`, `deleted`),
    -- M19修复：移除低效的idx_name索引，LIKE '%keyword%'无法使用前缀索引
    -- 复合索引：用于按分类+热度查询
    KEY `idx_category_hot` (`category_id`, `play_count` DESC),
    -- 复合索引：用于按歌手+状态查询
    KEY `idx_singer_status` (`singer_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='歌曲表';


-- ========================================================
-- 4. 包厢表 t_room
-- ========================================================
DROP TABLE IF EXISTS `t_room`;

CREATE TABLE `t_room` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(50) NOT NULL COMMENT '包厢名称（如：A01、豪华1号）',
    `type` VARCHAR(20) NOT NULL COMMENT '类型：小包/中包/大包/豪华包',
    `capacity` INT DEFAULT 4 COMMENT '容纳人数',
    `price_per_hour` DECIMAL(10,2) DEFAULT 0.00 COMMENT '每小时价格（元）',
    `min_consumption` DECIMAL(10,2) DEFAULT 0.00 COMMENT '最低消费（元）',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0空闲 1使用中 2清洁中 3维修中',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_type` (`type`),
    KEY `idx_status_deleted` (`status`, `deleted`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='包厢表';


-- ========================================================
-- 5. 订单表 t_order
-- ========================================================
DROP TABLE IF EXISTS `t_order`;

CREATE TABLE `t_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no` VARCHAR(50) NOT NULL COMMENT '订单编号（如：KT202603300001）',
    `room_id` BIGINT NOT NULL COMMENT '包厢ID',
    `start_time` DATETIME NOT NULL COMMENT '开台时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
    `duration_minutes` INT DEFAULT 0 COMMENT '消费时长（分钟）',
    `room_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '包厢费用',
    `total_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '总费用',
    `status` TINYINT DEFAULT 1 COMMENT '状态：1消费中 2已结账 3已取消',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `operator_id` BIGINT DEFAULT NULL COMMENT '操作员ID（开台人）',
    `closer_id` BIGINT DEFAULT NULL COMMENT '结账操作员ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    -- BugD2修复：Order 实体继承 BaseEntity，BaseEntity 有 @TableLogic deleted 字段，
    -- MyBatis-Plus 会自动在所有查询中附加 deleted=0，OrderMapper.xml 的 Base_Column_List 也包含 o.deleted，
    -- 缺少此字段会导致所有订单查询报 "Unknown column 'deleted' in 'where clause'" 致命错误
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_room_id` (`room_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_start_time` (`start_time`),
    -- 复合索引：用于查询某包厢的历史订单
    KEY `idx_room_create` (`room_id`, `create_time` DESC),
    -- 复合索引：用于按状态+时间查询
    KEY `idx_status_create` (`status`, `create_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消费订单表';


-- ========================================================
-- 6. 点歌记录表 t_order_song
-- ========================================================
DROP TABLE IF EXISTS `t_order_song`;

CREATE TABLE `t_order_song` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `song_id` BIGINT NOT NULL COMMENT '歌曲ID',
    `song_name` VARCHAR(200) DEFAULT NULL COMMENT '歌曲名（冗余，防止歌曲被删除后无法显示）',
    `singer_name` VARCHAR(100) DEFAULT NULL COMMENT '歌手名（冗余）',
    `sort_order` INT DEFAULT 0 COMMENT '排序序号（越小越靠前）',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0等待 1播放中 2已播放 3已跳过',
    `play_time` DATETIME DEFAULT NULL COMMENT '开始播放时间',
    `finish_time` DATETIME DEFAULT NULL COMMENT '播放结束时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '点歌时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_song_id` (`song_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_deleted` (`deleted`),
    -- 复合索引：用于查询某订单的点歌列表（按状态+排序）
    KEY `idx_order_status_sort` (`order_id`, `status`, `sort_order`),
    -- 复合索引：用于查询某订单的已点/已唱列表
    KEY `idx_order_create` (`order_id`, `create_time` DESC),
    -- L8修复：添加(order_id, song_id)唯一约束，防止同一订单重复点同一首歌
    UNIQUE KEY `uk_order_song` (`order_id`, `song_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='点歌记录表';


-- ========================================================
-- 7. 系统用户表 t_sys_user
-- ========================================================
DROP TABLE IF EXISTS `t_sys_user`;

CREATE TABLE `t_sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(200) NOT NULL COMMENT '密码（BCrypt加密）',
    `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
    `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    `role` VARCHAR(20) DEFAULT 'admin' COMMENT '角色：super_admin/admin',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0禁用 1启用',
    `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_status_deleted` (`status`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';


-- ========================================================
-- 8. 操作日志表 t_operation_log（可选，用于审计）
-- ========================================================
DROP TABLE IF EXISTS `t_operation_log`;

CREATE TABLE `t_operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '操作人用户名（冗余）',
    `module` VARCHAR(50) DEFAULT NULL COMMENT '操作模块',
    `operation` VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
    `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
    `request_params` TEXT DEFAULT NULL COMMENT '请求参数（JSON）',
    `response_data` TEXT DEFAULT NULL COMMENT '响应数据（JSON，可选）',
    `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
    `execute_time` INT DEFAULT 0 COMMENT '执行时长（毫秒）',
    `status` TINYINT DEFAULT 1 COMMENT '状态：0失败 1成功',
    `error_msg` VARCHAR(2000) DEFAULT NULL COMMENT '错误信息',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    -- SQL-S2修复：补充 deleted 字段，与其他表逻辑删除规范保持一致
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_module` (`module`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_status` (`status`),
    -- SQL-S7修复：补充 idx_deleted 索引
    KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';


-- ========================================================
-- 索引优化说明
-- ========================================================
/*
索引设计原则：
1. 所有外键字段（singer_id, category_id, room_id, order_id, song_id）都建立了索引
2. 频繁用于查询条件的字段（status, deleted, language, region, type）建立了索引
3. 排序字段（sort_order, play_count, create_time）建立了索引
4. 复合索引遵循最左前缀原则，用于优化多条件查询
5. 拼音首字母索引用于快速拼音搜索

关键复合索引说明：
- idx_category_hot: 分类+热度查询（如：查看某分类的热门歌曲）
- idx_singer_status: 歌手+状态查询（如：查看某歌手的上架歌曲）
- idx_room_create: 包厢+时间查询（如：查看某包厢的历史订单）
- idx_order_status_sort: 订单+状态+排序（如：查看某订单的待播放列表）
- idx_status_deleted: 状态+删除标记（通用查询条件）
*/
