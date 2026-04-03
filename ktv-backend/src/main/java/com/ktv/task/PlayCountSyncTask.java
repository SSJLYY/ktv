package com.ktv.task;

import com.ktv.service.HotSongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 点播次数同步定时任务
 * 每天凌晨00:00将Redis中的热门分数同步到数据库
 * SQL-N5修复：使用 Redis SETNX 实现轻量级分布式锁，防止多节点重复执行
 * H18修复：增加锁持有者验证，防止误释放其他节点的锁
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayCountSyncTask {

    private final HotSongService hotSongService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String LOCK_KEY = "ktv:lock:play_count_sync";
    private static final Duration LOCK_TTL = Duration.ofMinutes(30);

    /**
     * 每天凌晨00:00执行
     * 将Redis ZSet中的热门分数同步到数据库的play_count字段
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void syncPlayCountToDb() {
        // H18修复：使用唯一标识作为锁的值，用于验证锁持有者
        String lockValue = UUID.randomUUID().toString();
        
        // SQL-N5修复：使用 Redis SETNX 获取分布式锁，防止集群环境下多节点重复执行
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, lockValue, LOCK_TTL);
        if (acquired == null || !acquired) {
            log.info("定时任务：未获取到分布式锁，跳过本次执行（可能被其他节点执行中）");
            return;
        }
        
        try {
            log.info("定时任务：开始同步热门分数到数据库");
            hotSongService.syncHotScoreToDb();
            log.info("定时任务：热门分数同步完成");
        } catch (Exception e) {
            log.error("定时任务：热门分数同步失败", e);
        } finally {
            // H18修复：验证锁持有者后再释放，防止误释放其他节点的锁
            try {
                String currentLockValue = stringRedisTemplate.opsForValue().get(LOCK_KEY);
                if (lockValue.equals(currentLockValue)) {
                    stringRedisTemplate.delete(LOCK_KEY);
                    log.debug("定时任务：分布式锁释放成功");
                } else {
                    log.warn("定时任务：锁持有者已变更，跳过释放（锁将在TTL后自动过期）");
                }
            } catch (Exception e) {
                log.warn("释放分布式锁失败（锁将在TTL后自动过期）: {}", e.getMessage());
            }
        }
    }
}
