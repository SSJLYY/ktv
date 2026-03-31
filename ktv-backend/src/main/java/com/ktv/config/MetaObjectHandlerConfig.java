package com.ktv.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充配置
 *
 * BugB修复：移除 @Component 注解，避免与 MyBatisPlusConfig.metaObjectHandler() 产生
 * 重复 Bean 冲突（NoUniqueBeanDefinitionException）。
 * 统一由 MyBatisPlusConfig 注册，该版本同时填充 deleted 字段，功能更完整。
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Slf4j
public class MetaObjectHandlerConfig implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始插入填充...");
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始更新填充...");
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
