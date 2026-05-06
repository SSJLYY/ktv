package com.ktv.task;

import com.ktv.constant.RedisKeyConstants;
import com.ktv.service.HotSongService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * 每天将 Redis 中的热度分数同步回数据库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayCountSyncTask {

    private static final Duration LOCK_TTL = Duration.ofMinutes(30);
    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """,
            Long.class
    );

    private final HotSongService hotSongService;
    private final StringRedisTemplate stringRedisTemplate;

    @Scheduled(cron = "0 0 0 * * ?")
    public void syncPlayCountToDb() {
        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisKeyConstants.LOCK_PLAY_COUNT_SYNC, lockValue, LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            log.info("播放次数同步任务跳过: 未获取到分布式锁");
            return;
        }

        try {
            log.info("播放次数同步任务开始执行");
            hotSongService.syncHotScoreToDb();
            log.info("播放次数同步任务执行完成");
        } catch (Exception e) {
            log.error("播放次数同步任务执行失败", e);
        } finally {
            releaseLock(lockValue);
        }
    }

    private void releaseLock(String lockValue) {
        try {
            Long released = stringRedisTemplate.execute(
                    RELEASE_LOCK_SCRIPT,
                    Collections.singletonList(RedisKeyConstants.LOCK_PLAY_COUNT_SYNC),
                    lockValue
            );
            if (Long.valueOf(1L).equals(released)) {
                log.debug("播放次数同步任务已释放分布式锁");
            } else {
                log.warn("播放次数同步任务未释放锁: 锁已过期或已被其他节点接管");
            }
        } catch (Exception e) {
            log.warn("播放次数同步任务释放锁失败: {}", e.getMessage());
        }
    }
}
