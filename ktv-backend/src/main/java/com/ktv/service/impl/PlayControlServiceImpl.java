package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ktv.common.enums.OrderSongStatusEnum;
import com.ktv.common.exception.BusinessException;
import com.ktv.constant.RedisKeyConstants;
import com.ktv.dto.CurrentPlayVO;
import com.ktv.entity.Order;
import com.ktv.entity.OrderSong;
import com.ktv.mapper.OrderMapper;
import com.ktv.mapper.OrderSongMapper;
import com.ktv.service.PlayControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayControlServiceImpl implements PlayControlService {

    private static final String PLAYING = "PLAYING";
    private static final String PAUSED = "PAUSED";
    private static final String NONE = "NONE";
    private static final long KEY_TTL_HOURS = 24;

    private final OrderMapper orderMapper;
    private final OrderSongMapper orderSongMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void next(Long orderId) {
        assertActiveOrder(orderId);
        log.info("切歌: orderId={}", orderId);

        OrderSong currentSong = resolveCurrentPlayingSong(orderId, true);
        if (currentSong != null && currentSong.isPlaying()) {
            currentSong.setStatus(OrderSongStatusEnum.PLAYED.getCode());
            currentSong.setFinishTime(LocalDateTime.now());
            orderSongMapper.updateById(currentSong);
        }

        OrderSong nextSong = findNextWaitingSong(orderId);
        if (nextSong == null) {
            registerPlaybackStateRefresh(orderId, null, NONE, true);
            log.info("切歌完成: 队列为空, orderId={}", orderId);
            return;
        }

        nextSong.setStatus(OrderSongStatusEnum.PLAYING.getCode());
        nextSong.setPlayTime(LocalDateTime.now());
        nextSong.setFinishTime(null);
        orderSongMapper.updateById(nextSong);

        registerPlaybackStateRefresh(orderId, nextSong.getId(), PLAYING, true);
        log.info("切歌成功: orderId={}, orderSongId={}, songName={}", orderId, nextSong.getId(), nextSong.getSongName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replay(Long orderId) {
        assertActiveOrder(orderId);
        log.info("重唱: orderId={}", orderId);

        OrderSong currentSong = resolveCurrentPlayingSong(orderId, true);
        if (currentSong == null) {
            throw new BusinessException("当前没有播放的歌曲");
        }

        currentSong.setStatus(OrderSongStatusEnum.PLAYING.getCode());
        currentSong.setPlayTime(LocalDateTime.now());
        currentSong.setFinishTime(null);
        orderSongMapper.updateById(currentSong);

        registerPlaybackStateRefresh(orderId, currentSong.getId(), PLAYING, false);
        log.info("重唱成功: orderId={}, orderSongId={}", orderId, currentSong.getId());
    }

    @Override
    public void pause(Long orderId) {
        assertActiveOrder(orderId);
        log.info("暂停播放: orderId={}", orderId);

        OrderSong currentSong = resolveCurrentPlayingSong(orderId, true);
        if (currentSong == null) {
            throw new BusinessException("当前没有播放的歌曲");
        }

        writePlaybackState(orderId, currentSong.getId(), PAUSED);
        log.info("暂停播放成功: orderId={}, orderSongId={}", orderId, currentSong.getId());
    }

    @Override
    public void resume(Long orderId) {
        assertActiveOrder(orderId);
        log.info("恢复播放: orderId={}", orderId);

        OrderSong currentSong = resolveCurrentPlayingSong(orderId, true);
        if (currentSong == null) {
            throw new BusinessException("当前没有播放的歌曲");
        }

        writePlaybackState(orderId, currentSong.getId(), PLAYING);
        log.info("恢复播放成功: orderId={}, orderSongId={}", orderId, currentSong.getId());
    }

    @Override
    public CurrentPlayVO getCurrentPlayStatus(Long orderId) {
        assertActiveOrder(orderId);
        log.info("查询当前播放状态: orderId={}", orderId);

        CurrentPlayVO vo = new CurrentPlayVO();
        OrderSong currentSong = resolveCurrentPlayingSong(orderId, true);
        if (currentSong == null) {
            clearCurrentPlaybackState(orderId);
            vo.setPlayStatus(NONE);
        } else {
            String statusKey = RedisKeyConstants.buildPlayStatusKey(orderId);
            String playStatus = redisTemplate.opsForValue().get(statusKey);
            vo.setPlayStatus(playStatus != null ? playStatus : PLAYING);
            vo.setOrderSongId(currentSong.getId());
            vo.setSongId(currentSong.getSongId());
            vo.setSongName(currentSong.getSongName());
            vo.setSingerName(currentSong.getSingerName());
            vo.setDuration(currentSong.getDuration());
            vo.setFilePath(currentSong.getFilePath());
            vo.setPlayTime(currentSong.getPlayTime());
        }

        Long waitingCount = orderSongMapper.selectCount(new LambdaQueryWrapper<OrderSong>()
                .eq(OrderSong::getOrderId, orderId)
                .eq(OrderSong::getStatus, OrderSongStatusEnum.WAITING.getCode()));
        vo.setQueueRemaining(waitingCount != null ? waitingCount.intValue() : 0);
        return vo;
    }

    private void assertActiveOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.isActive()) {
            clearCurrentPlaybackState(orderId);
            redisTemplate.delete(RedisKeyConstants.buildQueueKey(orderId));
            throw new BusinessException("该订单不在进行中");
        }
    }

    private OrderSong resolveCurrentPlayingSong(Long orderId, boolean repairRedisState) {
        String playingKey = RedisKeyConstants.buildPlayingKey(orderId);
        Long currentOrderSongId = parsePlayingOrderSongId(orderId, playingKey);
        if (currentOrderSongId != null) {
            OrderSong song = orderSongMapper.findSongInfoById(currentOrderSongId);
            if (song != null && orderId.equals(song.getOrderId()) && song.isPlaying()) {
                return song;
            }
        }

        OrderSong fallbackSong = orderSongMapper.findSongInfoById(selectCurrentPlayingRecordId(orderId));
        if (fallbackSong != null && repairRedisState) {
            writePlaybackState(orderId, fallbackSong.getId(), PLAYING);
        }
        return fallbackSong;
    }

    private Long selectCurrentPlayingRecordId(Long orderId) {
        OrderSong currentSong = orderSongMapper.selectOne(new LambdaQueryWrapper<OrderSong>()
                .select(OrderSong::getId, OrderSong::getOrderId, OrderSong::getStatus, OrderSong::getPlayTime)
                .eq(OrderSong::getOrderId, orderId)
                .eq(OrderSong::getStatus, OrderSongStatusEnum.PLAYING.getCode())
                .orderByDesc(OrderSong::getPlayTime)
                .orderByDesc(OrderSong::getId)
                .last("LIMIT 1"));
        return currentSong != null ? currentSong.getId() : null;
    }

    private OrderSong findNextWaitingSong(Long orderId) {
        return orderSongMapper.selectOne(new LambdaQueryWrapper<OrderSong>()
                .eq(OrderSong::getOrderId, orderId)
                .eq(OrderSong::getStatus, OrderSongStatusEnum.WAITING.getCode())
                .orderByAsc(OrderSong::getSortOrder)
                .orderByAsc(OrderSong::getCreateTime)
                .orderByAsc(OrderSong::getId)
                .last("LIMIT 1"));
    }

    private Long parsePlayingOrderSongId(Long orderId, String playingKey) {
        String currentOrderSongIdStr = redisTemplate.opsForValue().get(playingKey);
        if (currentOrderSongIdStr == null || currentOrderSongIdStr.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(currentOrderSongIdStr);
        } catch (NumberFormatException e) {
            log.warn("播放记录格式错误: orderId={}, value={}", orderId, currentOrderSongIdStr);
            clearCurrentPlaybackState(orderId);
            return null;
        }
    }

    private void registerPlaybackStateRefresh(Long orderId, Long orderSongId, String status, boolean refreshQueue) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            writePlaybackState(orderId, orderSongId, status);
            if (refreshQueue) {
                refreshQueueCache(orderId);
            }
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                writePlaybackState(orderId, orderSongId, status);
                if (refreshQueue) {
                    refreshQueueCache(orderId);
                }
            }
        });
    }

    private void writePlaybackState(Long orderId, Long orderSongId, String status) {
        String playingKey = RedisKeyConstants.buildPlayingKey(orderId);
        String statusKey = RedisKeyConstants.buildPlayStatusKey(orderId);

        if (orderSongId == null) {
            redisTemplate.delete(playingKey);
            redisTemplate.opsForValue().set(statusKey, NONE, KEY_TTL_HOURS, TimeUnit.HOURS);
            return;
        }

        redisTemplate.opsForValue().set(playingKey, String.valueOf(orderSongId), KEY_TTL_HOURS, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(statusKey, status, KEY_TTL_HOURS, TimeUnit.HOURS);
    }

    private void refreshQueueCache(Long orderId) {
        String queueKey = RedisKeyConstants.buildQueueKey(orderId);
        List<OrderSong> waitingSongs = orderSongMapper.selectList(new LambdaQueryWrapper<OrderSong>()
                .select(OrderSong::getId)
                .eq(OrderSong::getOrderId, orderId)
                .eq(OrderSong::getStatus, OrderSongStatusEnum.WAITING.getCode())
                .orderByAsc(OrderSong::getSortOrder)
                .orderByAsc(OrderSong::getCreateTime)
                .orderByAsc(OrderSong::getId));

        redisTemplate.delete(queueKey);
        if (waitingSongs.isEmpty()) {
            return;
        }

        String[] ids = waitingSongs.stream()
                .map(OrderSong::getId)
                .map(String::valueOf)
                .toArray(String[]::new);
        redisTemplate.opsForList().rightPushAll(queueKey, ids);
        redisTemplate.expire(queueKey, KEY_TTL_HOURS, TimeUnit.HOURS);
    }

    private void clearCurrentPlaybackState(Long orderId) {
        redisTemplate.delete(RedisKeyConstants.buildPlayingKey(orderId));
        redisTemplate.opsForValue().set(RedisKeyConstants.buildPlayStatusKey(orderId), NONE, KEY_TTL_HOURS, TimeUnit.HOURS);
    }
}
