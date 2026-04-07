package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ktv.common.enums.OrderSongStatusEnum;
import com.ktv.dto.CurrentPlayVO;
import com.ktv.entity.OrderSong;
import com.ktv.common.exception.BusinessException;
import com.ktv.mapper.OrderSongMapper;
import com.ktv.service.PlayControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 播放控制Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayControlServiceImpl implements PlayControlService {

    private final OrderSongMapper orderSongMapper;
    private final StringRedisTemplate redisTemplate;

    /**
     * Redis Key常量
     */
    private static final String PLAYING_KEY_PREFIX = "ktv:playing:";
    private static final String PLAY_STATUS_KEY_PREFIX = "ktv:play:status:";
    private static final String QUEUE_KEY_PREFIX = "ktv:queue:";

    /**
     * 播放状态常量
     */
    private static final String PLAYING = "PLAYING";
    private static final String PAUSED = "PAUSED";
    private static final String NONE = "NONE";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void next(Long orderId) {
        log.info("切歌，orderId={}", orderId);

        // 1. 获取当前播放的点歌记录ID（S5修复：实际存的是 orderSongId 而非 songId）
        String playingKey = PLAYING_KEY_PREFIX + orderId;
        String currentOrderSongIdStr = redisTemplate.opsForValue().get(playingKey);
        Long currentOrderSongId = null;
        
        // H10修复：增加try-catch处理NumberFormatException
        if (currentOrderSongIdStr != null) {
            try {
                currentOrderSongId = Long.parseLong(currentOrderSongIdStr);
            } catch (NumberFormatException e) {
                log.warn("当前播放记录ID格式错误，orderId={}, value={}", orderId, currentOrderSongIdStr);
                redisTemplate.delete(playingKey); // 清除脏数据
            }
        }

        // 2. 如果有当前歌曲，标记为"已播放"
        if (currentOrderSongId != null) {
            OrderSong currentSong = orderSongMapper.selectById(currentOrderSongId);
            if (currentSong != null && currentSong.isPlaying()) {
                currentSong.setStatus(OrderSongStatusEnum.PLAYED.getCode());
                currentSong.setFinishTime(LocalDateTime.now());
                orderSongMapper.updateById(currentSong);
            }
        }

        // 3. 从Redis队列中取下一首
        String queueKey = QUEUE_KEY_PREFIX + orderId;
        String nextSongIdStr = redisTemplate.opsForList().leftPop(queueKey);

        if (nextSongIdStr == null) {
            // 队列为空，清除当前播放歌曲和播放状态
            // Bug11修复：同时清除 play:status key，否则 playStatus=PLAYING 但 songId=null，状态不一致
            redisTemplate.delete(playingKey);
            String statusKey = PLAY_STATUS_KEY_PREFIX + orderId;
            redisTemplate.opsForValue().set(statusKey, NONE, 24, TimeUnit.HOURS);
            log.info("队列为空，已清除播放状态，orderId={}", orderId);
            return;
        }

        // H11修复：增加try-catch处理NumberFormatException
        Long nextSongId;
        try {
            nextSongId = Long.parseLong(nextSongIdStr);
        } catch (NumberFormatException e) {
            log.warn("队列中歌曲ID格式错误，orderId={}, value={}", orderId, nextSongIdStr);
            return;
        }

        // 4. 将下一首设为"播放中"
        OrderSong nextSong = orderSongMapper.selectById(nextSongId);
        if (nextSong == null) {
            log.warn("下一首歌曲不存在，orderSongId={}", nextSongId);
            return;
        }

        nextSong.setStatus(OrderSongStatusEnum.PLAYING.getCode());
        nextSong.setPlayTime(LocalDateTime.now());
        orderSongMapper.updateById(nextSong);

        // 5. 更新Redis当前播放歌曲（存字符串）
        redisTemplate.opsForValue().set(playingKey, nextSongIdStr, 24, TimeUnit.HOURS);

        // 6. 恢复播放状态
        String statusKey = PLAY_STATUS_KEY_PREFIX + orderId;
        redisTemplate.opsForValue().set(statusKey, PLAYING, 24, TimeUnit.HOURS);

        log.info("切歌成功，下一首：orderSongId={}, songName={}", nextSongId, nextSong.getSongName());
    }

    @Override
    public void replay(Long orderId) {
        log.info("重唱，orderId={}", orderId);

        // 1. 获取当前播放的点歌记录ID（S5修复：实际存的是 orderSongId 而非 songId）
        String playingKey = PLAYING_KEY_PREFIX + orderId;
        String currentOrderSongIdStr = redisTemplate.opsForValue().get(playingKey);

        if (currentOrderSongIdStr == null) {
            throw new BusinessException("当前没有播放的歌曲");
        }

        // H15修复：增加try-catch处理NumberFormatException
        Long currentOrderSongId;
        try {
            currentOrderSongId = Long.parseLong(currentOrderSongIdStr);
        } catch (NumberFormatException e) {
            log.warn("当前播放记录ID格式错误，orderId={}, value={}", orderId, currentOrderSongIdStr);
            redisTemplate.delete(playingKey); // 清除脏数据
            throw new BusinessException("当前播放记录数据异常");
        }

        // 2. 查询当前歌曲
        OrderSong currentSong = orderSongMapper.selectById(currentOrderSongId);
        if (currentSong == null) {
            throw new BusinessException("当前歌曲不存在");
        }

        // 3. 重置播放时间
        currentSong.setPlayTime(LocalDateTime.now());
        orderSongMapper.updateById(currentSong);

        // 4. 恢复播放状态
        String statusKey = PLAY_STATUS_KEY_PREFIX + orderId;
        redisTemplate.opsForValue().set(statusKey, PLAYING, 24, TimeUnit.HOURS);

        log.info("重唱成功，orderSongId={}, songName={}", currentOrderSongId, currentSong.getSongName());
    }

    @Override
    public void pause(Long orderId) {
        log.info("暂停播放，orderId={}", orderId);

        // 更新Redis播放状态为"已暂停"
        String statusKey = PLAY_STATUS_KEY_PREFIX + orderId;
        redisTemplate.opsForValue().set(statusKey, PAUSED, 24, TimeUnit.HOURS);

        log.info("暂停播放成功，orderId={}", orderId);
    }

    @Override
    public void resume(Long orderId) {
        log.info("恢复播放，orderId={}", orderId);

        // 检查是否有播放歌曲
        String playingKey = PLAYING_KEY_PREFIX + orderId;
        String currentOrderSongIdStr = redisTemplate.opsForValue().get(playingKey);

        if (currentOrderSongIdStr == null) {
            throw new BusinessException("当前没有播放的歌曲");
        }

        // 更新Redis播放状态为"播放中"
        String statusKey = PLAY_STATUS_KEY_PREFIX + orderId;
        redisTemplate.opsForValue().set(statusKey, PLAYING, 24, TimeUnit.HOURS);

        log.info("恢复播放成功，orderId={}", orderId);
    }

    @Override
    public CurrentPlayVO getCurrentPlayStatus(Long orderId) {
        log.info("查询当前播放状态，orderId={}", orderId);

        CurrentPlayVO vo = new CurrentPlayVO();

        // 1. 获取播放状态（StringRedisTemplate直接返回String）
        String statusKey = PLAY_STATUS_KEY_PREFIX + orderId;
        String playStatus = redisTemplate.opsForValue().get(statusKey);
        vo.setPlayStatus(playStatus != null ? playStatus : NONE);

        // 2. 获取当前播放歌曲（S5修复：存的是 orderSongId 而非 songId）
        String playingKey = PLAYING_KEY_PREFIX + orderId;
        String currentOrderSongIdStr = redisTemplate.opsForValue().get(playingKey);

        if (currentOrderSongIdStr != null) {
            // H16修复：增加try-catch处理NumberFormatException
            Long currentOrderSongId;
            try {
                currentOrderSongId = Long.parseLong(currentOrderSongIdStr);
            } catch (NumberFormatException e) {
                log.warn("当前播放记录ID格式错误，orderId={}, value={}", orderId, currentOrderSongIdStr);
                redisTemplate.delete(playingKey); // 清除脏数据
                currentOrderSongId = null;
            }
            
            if (currentOrderSongId != null) {
                // 关联查询歌曲信息
                OrderSong orderSong = orderSongMapper.findSongInfoById(currentOrderSongId);
                if (orderSong != null) {
                    vo.setOrderSongId(orderSong.getId());
                    vo.setSongId(orderSong.getSongId());
                    vo.setSongName(orderSong.getSongName());
                    vo.setSingerName(orderSong.getSingerName());
                    vo.setDuration(orderSong.getDuration());
                    vo.setFilePath(orderSong.getFilePath());
                    vo.setPlayTime(orderSong.getPlayTime());
                }
            }
        }

        // 3. 获取队列剩余数量
        String queueKey = QUEUE_KEY_PREFIX + orderId;
        Long queueSize = redisTemplate.opsForList().size(queueKey);
        vo.setQueueRemaining(queueSize != null ? queueSize.intValue() : 0);

        return vo;
    }
}
