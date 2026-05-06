package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.common.enums.OrderSongStatusEnum;
import com.ktv.common.exception.BusinessException;
import com.ktv.constant.RedisKeyConstants;
import com.ktv.entity.Order;
import com.ktv.entity.OrderSong;
import com.ktv.mapper.OrderMapper;
import com.ktv.mapper.OrderSongMapper;
import com.ktv.mapper.SongMapper;
import com.ktv.service.HotSongService;
import com.ktv.service.PlayControlService;
import com.ktv.service.PlayQueueService;
import com.ktv.vo.SongVO;
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
public class PlayQueueServiceImpl implements PlayQueueService {

    private static final long QUEUE_EXPIRE_HOURS = 24;

    private final OrderMapper orderMapper;
    private final OrderSongMapper orderSongMapper;
    private final SongMapper songMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final HotSongService hotSongService;
    private final PlayControlService playControlService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addSongToQueue(Long orderId, Long songId) {
        assertActiveOrder(orderId);
        log.info("点歌: orderId={}, songId={}", orderId, songId);

        SongVO song = songMapper.selectVOById(songId);
        if (song == null) {
            throw new BusinessException("歌曲不存在");
        }

        Long waitingCount = orderSongMapper.selectCount(new LambdaQueryWrapper<OrderSong>()
                .eq(OrderSong::getOrderId, orderId)
                .eq(OrderSong::getStatus, OrderSongStatusEnum.WAITING.getCode()));
        int sortOrder = waitingCount != null ? waitingCount.intValue() + 1 : 1;

        OrderSong orderSong = new OrderSong();
        orderSong.setOrderId(orderId);
        orderSong.setSongId(songId);
        orderSong.setSongName(song.getName());
        orderSong.setSingerName(song.getSingerName());
        orderSong.setSortOrder(sortOrder);
        orderSong.setStatus(OrderSongStatusEnum.WAITING.getCode());
        orderSong.setCreateTime(LocalDateTime.now());
        orderSongMapper.insert(orderSong);

        if (hotSongService != null) {
            hotSongService.incrementHotScore(songId);
        }

        Long orderSongId = orderSong.getId();
        registerQueueRefreshAfterCommit(orderId, true);

        log.info("点歌成功: orderSongId={}, sortOrder={}", orderSongId, sortOrder);
        return orderSongId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void topSong(Long orderId, Long orderSongId) {
        assertActiveOrder(orderId);
        log.info("置顶歌曲: orderId={}, orderSongId={}", orderId, orderSongId);

        OrderSong orderSong = orderSongMapper.selectById(orderSongId);
        if (orderSong == null) {
            throw new BusinessException("点歌记录不存在");
        }
        if (!orderId.equals(orderSong.getOrderId())) {
            throw new BusinessException("点歌记录不属于该订单");
        }
        if (!orderSong.isWaiting()) {
            throw new BusinessException("只能置顶等待中的歌曲");
        }

        List<OrderSong> waitingSongs = listWaitingSongs(orderId);
        if (waitingSongs.isEmpty()) {
            return;
        }

        int nextSortOrder = 1;
        if (orderSong.getSortOrder() == null || orderSong.getSortOrder() != 1) {
            orderSong.setSortOrder(nextSortOrder++);
            orderSongMapper.updateById(orderSong);
        } else {
            nextSortOrder = 2;
        }

        for (OrderSong waitingSong : waitingSongs) {
            if (waitingSong.getId().equals(orderSongId)) {
                continue;
            }
            if (waitingSong.getSortOrder() == null || waitingSong.getSortOrder() != nextSortOrder) {
                waitingSong.setSortOrder(nextSortOrder);
                orderSongMapper.updateById(waitingSong);
            }
            nextSortOrder++;
        }

        registerQueueRefreshAfterCommit(orderId, false);
        log.info("置顶成功: orderSongId={}", orderSongId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeSong(Long orderId, Long orderSongId) {
        assertActiveOrder(orderId);
        log.info("取消点歌: orderId={}, orderSongId={}", orderId, orderSongId);

        OrderSong orderSong = orderSongMapper.selectById(orderSongId);
        if (orderSong == null) {
            throw new BusinessException("点歌记录不存在");
        }
        if (!orderId.equals(orderSong.getOrderId())) {
            throw new BusinessException("点歌记录不属于该订单");
        }
        if (!orderSong.isWaiting()) {
            throw new BusinessException("只能取消等待中的歌曲");
        }

        orderSongMapper.deleteById(orderSongId);
        normalizeWaitingSortOrder(orderId);
        registerQueueRefreshAfterCommit(orderId, false);

        log.info("取消点歌成功: orderSongId={}", orderSongId);
    }

    @Override
    public IPage<OrderSong> getQueueList(Page<OrderSong> page, Long orderId) {
        assertActiveOrder(orderId);
        log.info("查询等待队列: orderId={}", orderId);
        return orderSongMapper.selectByOrderIdAndStatus(page, orderId, OrderSongStatusEnum.WAITING.getCode());
    }

    @Override
    public IPage<OrderSong> getPlayedList(Page<OrderSong> page, Long orderId) {
        assertActiveOrder(orderId);
        log.info("查询已播列表: orderId={}", orderId);
        return orderSongMapper.selectPlayedByOrderId(page, orderId);
    }

    private void assertActiveOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.isActive()) {
            stringRedisTemplate.delete(RedisKeyConstants.buildQueueKey(orderId));
            clearPlaybackKeys(orderId);
            throw new BusinessException("该订单不在进行中");
        }
    }

    private List<OrderSong> listWaitingSongs(Long orderId) {
        return orderSongMapper.selectList(new LambdaQueryWrapper<OrderSong>()
                .eq(OrderSong::getOrderId, orderId)
                .eq(OrderSong::getStatus, OrderSongStatusEnum.WAITING.getCode())
                .orderByAsc(OrderSong::getSortOrder)
                .orderByAsc(OrderSong::getCreateTime)
                .orderByAsc(OrderSong::getId));
    }

    private void normalizeWaitingSortOrder(Long orderId) {
        List<OrderSong> waitingSongs = listWaitingSongs(orderId);
        int sortOrder = 1;
        for (OrderSong waitingSong : waitingSongs) {
            if (waitingSong.getSortOrder() == null || waitingSong.getSortOrder() != sortOrder) {
                waitingSong.setSortOrder(sortOrder);
                orderSongMapper.updateById(waitingSong);
            }
            sortOrder++;
        }
    }

    private void registerQueueRefreshAfterCommit(Long orderId, boolean autoStartPlayback) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            refreshQueueCache(orderId);
            if (autoStartPlayback) {
                triggerAutoStartPlayback(orderId);
            }
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                refreshQueueCache(orderId);
                if (autoStartPlayback) {
                    triggerAutoStartPlayback(orderId);
                }
            }
        });
    }

    private void refreshQueueCache(Long orderId) {
        String queueKey = RedisKeyConstants.buildQueueKey(orderId);
        List<OrderSong> waitingSongs = listWaitingSongs(orderId);
        stringRedisTemplate.delete(queueKey);
        if (waitingSongs.isEmpty()) {
            return;
        }

        String[] orderSongIds = waitingSongs.stream()
                .map(OrderSong::getId)
                .map(String::valueOf)
                .toArray(String[]::new);
        stringRedisTemplate.opsForList().rightPushAll(queueKey, orderSongIds);
        stringRedisTemplate.expire(queueKey, QUEUE_EXPIRE_HOURS, TimeUnit.HOURS);
    }

    private void triggerAutoStartPlayback(Long orderId) {
        try {
            if (playControlService != null && shouldAutoStartPlayback(orderId)) {
                log.info("当前没有有效播放歌曲，自动触发下一首: orderId={}", orderId);
                playControlService.next(orderId);
            }
        } catch (Exception e) {
            log.warn("自动触发播放失败，不影响点歌: orderId={}, error={}", orderId, e.getMessage());
        }
    }

    private boolean shouldAutoStartPlayback(Long orderId) {
        String playingKey = RedisKeyConstants.buildPlayingKey(orderId);
        String currentPlaying = stringRedisTemplate.opsForValue().get(playingKey);
        if (currentPlaying == null || currentPlaying.isBlank()) {
            return true;
        }

        Long currentOrderSongId;
        try {
            currentOrderSongId = Long.parseLong(currentPlaying);
        } catch (NumberFormatException e) {
            log.warn("自动播放前发现无效的播放记录: orderId={}, value={}", orderId, currentPlaying);
            clearPlaybackKeys(orderId);
            return true;
        }

        OrderSong currentOrderSong = orderSongMapper.selectById(currentOrderSongId);
        if (currentOrderSong == null || !orderId.equals(currentOrderSong.getOrderId()) || !currentOrderSong.isPlaying()) {
            log.warn("自动播放前发现失效的当前播放记录: orderId={}, orderSongId={}", orderId, currentOrderSongId);
            clearPlaybackKeys(orderId);
            return true;
        }
        return false;
    }

    private void clearPlaybackKeys(Long orderId) {
        stringRedisTemplate.delete(RedisKeyConstants.buildPlayingKey(orderId));
        stringRedisTemplate.delete(RedisKeyConstants.buildPlayStatusKey(orderId));
    }
}
