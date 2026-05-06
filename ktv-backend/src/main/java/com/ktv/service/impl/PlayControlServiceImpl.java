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

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayControlServiceImpl implements PlayControlService {

    private static final String PLAYING = "PLAYING";
    private static final String PAUSED = "PAUSED";
    private static final String NONE = "NONE";

    private final OrderMapper orderMapper;
    private final OrderSongMapper orderSongMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void next(Long orderId) {
        assertActiveOrder(orderId);
        log.info("切歌，orderId={}", orderId);

        String playingKey = RedisKeyConstants.buildPlayingKey(orderId);
        Long currentOrderSongId = parsePlayingOrderSongId(orderId, playingKey);

        if (currentOrderSongId != null) {
            OrderSong currentSong = orderSongMapper.selectById(currentOrderSongId);
            if (currentSong != null && currentSong.isPlaying()) {
                currentSong.setStatus(OrderSongStatusEnum.PLAYED.getCode());
                currentSong.setFinishTime(LocalDateTime.now());
                orderSongMapper.updateById(currentSong);
            }
        }

        String queueKey = RedisKeyConstants.buildQueueKey(orderId);
        OrderSong nextSong = pollNextPlayableSong(orderId, queueKey);
        if (nextSong == null) {
            clearCurrentPlaybackState(orderId, playingKey);
            log.info("队列为空或仅剩无效数据，已清除播放状态，orderId={}", orderId);
            return;
        }

        Long nextSongId = nextSong.getId();
        nextSong.setStatus(OrderSongStatusEnum.PLAYING.getCode());
        nextSong.setPlayTime(LocalDateTime.now());
        orderSongMapper.updateById(nextSong);

        redisTemplate.opsForValue().set(playingKey, String.valueOf(nextSongId), 24, TimeUnit.HOURS);
        String statusKey = RedisKeyConstants.buildPlayStatusKey(orderId);
        redisTemplate.opsForValue().set(statusKey, PLAYING, 24, TimeUnit.HOURS);

        log.info("切歌成功，下一首：orderSongId={}, songName={}", nextSongId, nextSong.getSongName());
    }

    @Override
    public void replay(Long orderId) {
        assertActiveOrder(orderId);
        log.info("重唱，orderId={}", orderId);

        String playingKey = RedisKeyConstants.buildPlayingKey(orderId);
        Long currentOrderSongId = parsePlayingOrderSongId(orderId, playingKey);
        if (currentOrderSongId == null) {
            throw new BusinessException("当前没有播放的歌曲");
        }

        OrderSong currentSong = orderSongMapper.selectById(currentOrderSongId);
        if (currentSong == null) {
            clearCurrentPlaybackState(orderId, playingKey);
            throw new BusinessException("当前歌曲不存在");
        }

        currentSong.setPlayTime(LocalDateTime.now());
        orderSongMapper.updateById(currentSong);

        String statusKey = RedisKeyConstants.buildPlayStatusKey(orderId);
        redisTemplate.opsForValue().set(statusKey, PLAYING, 24, TimeUnit.HOURS);

        log.info("重唱成功，orderSongId={}, songName={}", currentOrderSongId, currentSong.getSongName());
    }

    @Override
    public void pause(Long orderId) {
        assertActiveOrder(orderId);
        log.info("暂停播放，orderId={}", orderId);

        String playingKey = RedisKeyConstants.buildPlayingKey(orderId);
        Long currentOrderSongId = parsePlayingOrderSongId(orderId, playingKey);
        if (currentOrderSongId == null) {
            throw new BusinessException("当前没有播放的歌曲");
        }

        String statusKey = RedisKeyConstants.buildPlayStatusKey(orderId);
        redisTemplate.opsForValue().set(statusKey, PAUSED, 24, TimeUnit.HOURS);

        log.info("暂停播放成功，orderId={}", orderId);
    }

    @Override
    public void resume(Long orderId) {
        assertActiveOrder(orderId);
        log.info("恢复播放，orderId={}", orderId);

        String playingKey = RedisKeyConstants.buildPlayingKey(orderId);
        Long currentOrderSongId = parsePlayingOrderSongId(orderId, playingKey);
        if (currentOrderSongId == null) {
            throw new BusinessException("当前没有播放的歌曲");
        }

        String statusKey = RedisKeyConstants.buildPlayStatusKey(orderId);
        redisTemplate.opsForValue().set(statusKey, PLAYING, 24, TimeUnit.HOURS);

        log.info("恢复播放成功，orderId={}", orderId);
    }

    @Override
    public CurrentPlayVO getCurrentPlayStatus(Long orderId) {
        assertActiveOrder(orderId);
        log.info("查询当前播放状态，orderId={}", orderId);

        CurrentPlayVO vo = new CurrentPlayVO();
        String statusKey = RedisKeyConstants.buildPlayStatusKey(orderId);
        String playStatus = redisTemplate.opsForValue().get(statusKey);
        vo.setPlayStatus(playStatus != null ? playStatus : NONE);

        String playingKey = RedisKeyConstants.buildPlayingKey(orderId);
        Long currentOrderSongId = parsePlayingOrderSongId(orderId, playingKey);
        if (currentOrderSongId != null) {
            OrderSong orderSong = orderSongMapper.findSongInfoById(currentOrderSongId);
            if (orderSong != null) {
                vo.setOrderSongId(orderSong.getId());
                vo.setSongId(orderSong.getSongId());
                vo.setSongName(orderSong.getSongName());
                vo.setSingerName(orderSong.getSingerName());
                vo.setDuration(orderSong.getDuration());
                vo.setFilePath(orderSong.getFilePath());
                vo.setPlayTime(orderSong.getPlayTime());
            } else {
                clearCurrentPlaybackState(orderId, playingKey);
                vo.setPlayStatus(NONE);
            }
        } else {
            vo.setPlayStatus(NONE);
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
            clearCurrentPlaybackState(orderId, RedisKeyConstants.buildPlayingKey(orderId));
            redisTemplate.delete(RedisKeyConstants.buildQueueKey(orderId));
            throw new BusinessException("该订单不在进行中");
        }
    }

    private OrderSong pollNextPlayableSong(Long orderId, String queueKey) {
        while (true) {
            String nextSongIdStr = redisTemplate.opsForList().leftPop(queueKey);
            if (nextSongIdStr == null) {
                return null;
            }

            Long nextSongId;
            try {
                nextSongId = Long.parseLong(nextSongIdStr);
            } catch (NumberFormatException e) {
                log.warn("队列中歌曲ID格式错误，orderId={}, value={}", orderId, nextSongIdStr);
                continue;
            }

            OrderSong nextSong = orderSongMapper.selectById(nextSongId);
            if (nextSong == null) {
                log.warn("队列中的点歌记录不存在，orderId={}, orderSongId={}", orderId, nextSongId);
                continue;
            }
            if (!orderId.equals(nextSong.getOrderId())) {
                log.warn("队列中的点歌记录不属于当前订单，orderId={}, orderSongId={}, recordOrderId={}",
                        orderId, nextSongId, nextSong.getOrderId());
                continue;
            }
            if (!nextSong.isWaiting()) {
                log.warn("队列中的点歌记录状态不可播放，orderId={}, orderSongId={}, status={}",
                        orderId, nextSongId, nextSong.getStatus());
                continue;
            }
            return nextSong;
        }
    }

    private Long parsePlayingOrderSongId(Long orderId, String playingKey) {
        String currentOrderSongIdStr = redisTemplate.opsForValue().get(playingKey);
        if (currentOrderSongIdStr == null) {
            return null;
        }

        try {
            return Long.parseLong(currentOrderSongIdStr);
        } catch (NumberFormatException e) {
            log.warn("当前播放记录ID格式错误，orderId={}, value={}", orderId, currentOrderSongIdStr);
            clearCurrentPlaybackState(orderId, playingKey);
            return null;
        }
    }

    private void clearCurrentPlaybackState(Long orderId, String playingKey) {
        redisTemplate.delete(playingKey);
        String statusKey = RedisKeyConstants.buildPlayStatusKey(orderId);
        redisTemplate.opsForValue().set(statusKey, NONE, 24, TimeUnit.HOURS);
    }
}
