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
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayQueueServiceImpl implements PlayQueueService {

    private static final long QUEUE_EXPIRE_HOURS = 24;

    private final OrderMapper orderMapper;
    private final OrderSongMapper orderSongMapper;
    private final SongMapper songMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisLockRegistry redisLockRegistry;
    private final HotSongService hotSongService;
    private final PlayControlService playControlService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addSongToQueue(Long orderId, Long songId) {
        validatePositiveId(orderId, "订单ID不能为空");
        validatePositiveId(songId, "歌曲ID不能为空");
        return withQueueMutationLock(orderId, () -> {
            assertActiveOrderForQueueMutation(orderId);
            log.info("添加点歌: orderId={}, songId={}", orderId, songId);

            SongVO song = songMapper.selectVOById(songId);
            if (song == null) {
                throw new BusinessException("歌曲不存在");
            }

            int sortOrder = orderSongMapper.selectList(new LambdaQueryWrapper<OrderSong>()
                            .select(OrderSong::getSortOrder)
                            .eq(OrderSong::getOrderId, orderId)
                            .eq(OrderSong::getStatus, OrderSongStatusEnum.WAITING.getCode()))
                    .stream()
                    .map(OrderSong::getSortOrder)
                    .filter(value -> value != null && value > 0)
                    .max(Integer::compareTo)
                    .orElse(0) + 1;

            OrderSong orderSong = new OrderSong();
            orderSong.setOrderId(orderId);
            orderSong.setSongId(songId);
            orderSong.setSongName(song.getName());
            orderSong.setSingerName(song.getSingerName());
            orderSong.setSortOrder(sortOrder);
            orderSong.setStatus(OrderSongStatusEnum.WAITING.getCode());
            orderSong.setCreateTime(LocalDateTime.now());

            if (orderSongMapper.insert(orderSong) <= 0) {
                throw new BusinessException("点歌失败");
            }

            hotSongService.incrementHotScore(songId);
            registerQueueRefreshAfterCommit(orderId, true);
            log.info("点歌成功: orderSongId={}, sortOrder={}", orderSong.getId(), sortOrder);
            return orderSong.getId();
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void topSong(Long orderId, Long orderSongId) {
        validatePositiveId(orderId, "订单ID不能为空");
        validatePositiveId(orderSongId, "点歌记录ID不能为空");
        withQueueMutationLock(orderId, () -> {
            assertActiveOrderForQueueMutation(orderId);
            log.info("置顶歌曲: orderId={}, orderSongId={}", orderId, orderSongId);

            OrderSong orderSong = loadOrderSong(orderSongId);
            if (!orderId.equals(orderSong.getOrderId())) {
                throw new BusinessException("点歌记录不属于当前订单");
            }
            if (!orderSong.isWaiting()) {
                throw new BusinessException("只能置顶等待中的歌曲");
            }

            List<OrderSong> waitingSongs = listWaitingSongs(orderId);
            if (waitingSongs.isEmpty()) {
                return null;
            }

            int nextSortOrder = 1;
            if (orderSong.getSortOrder() == null || orderSong.getSortOrder() != 1) {
                orderSong.setSortOrder(nextSortOrder++);
                updateOrderSong(orderSong, "置顶歌曲失败");
            } else {
                nextSortOrder = 2;
            }

            for (OrderSong waitingSong : waitingSongs) {
                if (waitingSong.getId().equals(orderSongId)) {
                    continue;
                }
                if (waitingSong.getSortOrder() == null || waitingSong.getSortOrder() != nextSortOrder) {
                    waitingSong.setSortOrder(nextSortOrder);
                    updateOrderSong(waitingSong, "重排点歌顺序失败");
                }
                nextSortOrder++;
            }

            registerQueueRefreshAfterCommit(orderId, false);
            log.info("置顶成功: orderSongId={}", orderSongId);
            return null;
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeSong(Long orderId, Long orderSongId) {
        validatePositiveId(orderId, "订单ID不能为空");
        validatePositiveId(orderSongId, "点歌记录ID不能为空");
        withQueueMutationLock(orderId, () -> {
            assertActiveOrderForQueueMutation(orderId);
            log.info("取消点歌: orderId={}, orderSongId={}", orderId, orderSongId);

            OrderSong orderSong = loadOrderSong(orderSongId);
            if (!orderId.equals(orderSong.getOrderId())) {
                throw new BusinessException("点歌记录不属于当前订单");
            }
            if (!orderSong.isWaiting()) {
                throw new BusinessException("只能取消等待中的歌曲");
            }

            if (orderSongMapper.deleteById(orderSongId) <= 0) {
                throw new BusinessException("取消点歌失败");
            }

            normalizeWaitingSortOrder(orderId);
            registerQueueRefreshAfterCommit(orderId, false);
            log.info("取消点歌成功: orderSongId={}", orderSongId);
            return null;
        });
    }

    @Override
    public IPage<OrderSong> getQueueList(Page<OrderSong> page, Long orderId) {
        validatePositiveId(orderId, "订单ID不能为空");
        assertActiveOrder(orderId);
        log.info("查询等待队列: orderId={}", orderId);
        return orderSongMapper.selectByOrderIdAndStatus(page, orderId, OrderSongStatusEnum.WAITING.getCode());
    }

    @Override
    public IPage<OrderSong> getPlayedList(Page<OrderSong> page, Long orderId) {
        validatePositiveId(orderId, "订单ID不能为空");
        assertActiveOrder(orderId);
        log.info("查询已唱列表: orderId={}", orderId);
        return orderSongMapper.selectPlayedByOrderId(page, orderId);
    }

    private void validatePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(message);
        }
    }

    private void assertActiveOrder(Long orderId) {
        assertActiveOrder(orderId, orderMapper.selectById(orderId));
    }

    private void assertActiveOrderForQueueMutation(Long orderId) {
        assertActiveOrder(orderId, orderMapper.selectByIdForUpdate(orderId));
    }

    private void assertActiveOrder(Long orderId, Order order) {
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.isActive()) {
            stringRedisTemplate.delete(RedisKeyConstants.buildQueueKey(orderId));
            clearPlaybackKeys(orderId);
            throw new BusinessException("当前订单未处于进行中状态");
        }
    }

    private OrderSong loadOrderSong(Long orderSongId) {
        OrderSong orderSong = orderSongMapper.selectById(orderSongId);
        if (orderSong == null) {
            throw new BusinessException("点歌记录不存在");
        }
        return orderSong;
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
                updateOrderSong(waitingSong, "重排点歌顺序失败");
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
            if (shouldAutoStartPlayback(orderId)) {
                log.info("当前没有有效播放歌曲，自动触发下一首: orderId={}", orderId);
                playControlService.next(orderId);
            }
        } catch (Exception e) {
            log.warn("自动触发播放失败，不影响点歌: orderId={}, error={}", orderId, e.getMessage(), e);
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

    private void updateOrderSong(OrderSong orderSong, String failureMessage) {
        if (orderSongMapper.updateById(orderSong) <= 0) {
            throw new BusinessException(failureMessage);
        }
    }

    private <T> T withQueueMutationLock(Long orderId, Callable<T> action) {
        String lockKey = "lock:play_queue:order:" + orderId;
        Lock lock = redisLockRegistry.obtain(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(10, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException("点歌队列操作繁忙，请稍后重试");
            }
            return action.call();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("点歌队列操作已中断，请重试");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("点歌队列操作失败: orderId={}, error={}", orderId, e.getMessage(), e);
            throw new BusinessException("点歌队列操作失败，请稍后重试");
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }
}
