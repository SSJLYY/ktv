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
        log.info("点歌：订单ID={}, 歌曲ID={}", orderId, songId);

        SongVO song = songMapper.selectVOById(songId);
        if (song == null) {
            throw new BusinessException("歌曲不存在");
        }

        String queueKey = RedisKeyConstants.buildQueueKey(orderId);
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

        stringRedisTemplate.opsForList().rightPush(queueKey, orderSong.getId().toString());
        stringRedisTemplate.expire(queueKey, QUEUE_EXPIRE_HOURS, TimeUnit.HOURS);

        if (hotSongService != null) {
            hotSongService.incrementHotScore(songId);
        }

        final Long orderSongId = orderSong.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    if (playControlService != null && shouldAutoStartPlayback(orderId)) {
                        log.info("当前无有效播放歌曲，自动触发播放第一首：orderId={}", orderId);
                        playControlService.next(orderId);
                    }
                } catch (Exception e) {
                    log.warn("自动触发播放失败（不影响点歌），orderId={}: {}", orderId, e.getMessage());
                }
            }
        });

        log.info("点歌成功：点歌记录ID={}, 排序序号={}", orderSongId, sortOrder);
        return orderSongId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void topSong(Long orderId, Long orderSongId) {
        assertActiveOrder(orderId);
        log.info("置顶：订单ID={}, 点歌记录ID={}", orderId, orderSongId);

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

        String queueKey = RedisKeyConstants.buildQueueKey(orderId);
        stringRedisTemplate.opsForList().remove(queueKey, 1, orderSongId.toString());
        stringRedisTemplate.opsForList().leftPush(queueKey, orderSongId.toString());
        syncWaitingSortOrder(orderId, queueKey);

        log.info("置顶成功：点歌记录ID={}", orderSongId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeSong(Long orderId, Long orderSongId) {
        assertActiveOrder(orderId);
        log.info("取消点歌：订单ID={}, 点歌记录ID={}", orderId, orderSongId);

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

        String queueKey = RedisKeyConstants.buildQueueKey(orderId);
        stringRedisTemplate.opsForList().remove(queueKey, 1, orderSongId.toString());
        orderSongMapper.deleteById(orderSongId);
        syncWaitingSortOrder(orderId, queueKey);

        log.info("取消点歌成功：点歌记录ID={}", orderSongId);
    }

    @Override
    public IPage<OrderSong> getQueueList(Page<OrderSong> page, Long orderId) {
        assertActiveOrder(orderId);
        log.info("查询排队列表：订单ID={}", orderId);
        return orderSongMapper.selectByOrderIdAndStatus(page, orderId, OrderSongStatusEnum.WAITING.getCode());
    }

    @Override
    public IPage<OrderSong> getPlayedList(Page<OrderSong> page, Long orderId) {
        assertActiveOrder(orderId);
        log.info("查询已唱列表：订单ID={}", orderId);
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

    private void syncWaitingSortOrder(Long orderId, String queueKey) {
        List<String> queueSongIds = stringRedisTemplate.opsForList().range(queueKey, 0, -1);
        if (queueSongIds == null || queueSongIds.isEmpty()) {
            return;
        }

        int sortOrder = 1;
        for (String queueSongId : queueSongIds) {
            Long queuedOrderSongId;
            try {
                queuedOrderSongId = Long.parseLong(queueSongId);
            } catch (NumberFormatException e) {
                log.warn("重排队列时发现无效点歌记录ID，orderId={}, value={}", orderId, queueSongId);
                continue;
            }

            OrderSong queuedSong = orderSongMapper.selectById(queuedOrderSongId);
            if (queuedSong == null || !orderId.equals(queuedSong.getOrderId()) || !queuedSong.isWaiting()) {
                continue;
            }

            if (queuedSong.getSortOrder() == null || queuedSong.getSortOrder() != sortOrder) {
                queuedSong.setSortOrder(sortOrder);
                orderSongMapper.updateById(queuedSong);
            }
            sortOrder++;
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
            log.warn("自动播放前发现无效的playingKey，orderId={}, value={}", orderId, currentPlaying);
            clearPlaybackKeys(orderId);
            return true;
        }

        OrderSong currentOrderSong = orderSongMapper.selectById(currentOrderSongId);
        if (currentOrderSong == null || !orderId.equals(currentOrderSong.getOrderId()) || !currentOrderSong.isPlaying()) {
            log.warn("自动播放前发现失效的当前播放记录，orderId={}, orderSongId={}", orderId, currentOrderSongId);
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
