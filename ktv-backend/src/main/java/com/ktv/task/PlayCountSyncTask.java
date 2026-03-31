package com.ktv.task;

import com.ktv.service.HotSongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 点播次数同步定时任务
 * 每天凌晨00:00将Redis中的热门分数同步到数据库
 * SQL-N5修复：使用 Redis SETNX 实现轻量级分布式锁，防止多节点重复执行
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
        // SQL-N5修复：使用 Redis SETNX 获取分布式锁，防止集群环境下多节点重复执行
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(LOCK_KEY, "locked", LOCK_TTL);
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
            stringRedisTemplate.delete(LOCK_KEY);
        }
    }
}
