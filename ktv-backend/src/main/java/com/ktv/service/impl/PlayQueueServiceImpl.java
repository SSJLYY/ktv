package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.entity.OrderSong;
import com.ktv.common.exception.BusinessException;
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

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 点歌队列Service实现类
 * M15/M16修复：统一使用构造器注入，移除@RequiredArgsConstructor和@Autowired混合使用
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayQueueServiceImpl implements PlayQueueService {

    private final OrderSongMapper orderSongMapper;
    private final SongMapper songMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final HotSongService hotSongService;
    /**
     * Bug12修复：注入 PlayControlService（用setter注入避免循环依赖）
     * 点歌后如果当前没有播放中的歌曲，自动触发 next() 开始播放
     */
    private final PlayControlService playControlService;

    /**
     * Redis队列Key前缀
     */
    private static final String QUEUE_KEY_PREFIX = "ktv:queue:";
    /**
     * Redis当前播放Key前缀（与 PlayControlServiceImpl 保持一致）
     * Bug12修复：点歌时检查是否有歌在播放
     */
    private static final String PLAYING_KEY_PREFIX = "ktv:playing:";
    /**
     * Redis队列过期时间（24小时）
     */
    private static final long QUEUE_EXPIRE_HOURS = 24;

    /**
     * 点歌：添加歌曲到排队队列
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addSongToQueue(Long orderId, Long songId) {
        log.info("点歌：订单ID={}, 歌曲ID={}", orderId, songId);

        // 1. 查询歌曲信息（Bug14修复：用selectVOById获取singerName，Song实体无此字段）
        SongVO song = songMapper.selectVOById(songId);
        if (song == null) {
            throw new BusinessException("歌曲不存在");
        }

        // 2. 查询当前排队队列数量
        String queueKey = getQueueKey(orderId);
        Long queueSize = stringRedisTemplate.opsForList().size(queueKey);
        int sortOrder = queueSize != null ? queueSize.intValue() + 1 : 1;

        // 3. 创建点歌记录
        OrderSong orderSong = new OrderSong();
        orderSong.setOrderId(orderId);
        orderSong.setSongId(songId);
        orderSong.setSongName(song.getName());
        orderSong.setSingerName(song.getSingerName());
        orderSong.setSortOrder(sortOrder);
        orderSong.setStatus(0); // 0=等待中
        orderSong.setCreateTime(LocalDateTime.now());

        orderSongMapper.insert(orderSong);

        // 4. 将点歌记录ID推入Redis队列
        stringRedisTemplate.opsForList().rightPush(queueKey, orderSong.getId().toString());

        // 5. 设置队列过期时间
        stringRedisTemplate.expire(queueKey, QUEUE_EXPIRE_HOURS, TimeUnit.HOURS);

        // 6. 增加歌曲热度
        if (hotSongService != null) {
            hotSongService.incrementHotScore(songId);
        }

        // 7. H17修复：将playControlService.next()调用移到事务外
        // 使用TransactionSynchronization在事务提交后异步触发，避免事务传播问题
        final Long orderSongId = orderSong.getId();
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
            new org.springframework.transaction.support.TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        String playingKey = PLAYING_KEY_PREFIX + orderId;
                        String currentPlaying = stringRedisTemplate.opsForValue().get(playingKey);
                        if (currentPlaying == null && playControlService != null) {
                            log.info("当前无播放歌曲，自动触发播放第一首：orderId={}", orderId);
                            playControlService.next(orderId);
                        }
                    } catch (Exception e) {
                        log.warn("自动触发播放失败（不影响点歌），orderId={}: {}", orderId, e.getMessage());
                    }
                }
            }
        );

        log.info("点歌成功：点歌记录ID={}, 排序序号={}", orderSongId, sortOrder);
        return orderSongId;
    }

    /**
     * 置顶：将某首歌曲移到队列头部
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void topSong(Long orderId, Long orderSongId) {
        log.info("置顶：订单ID={}, 点歌记录ID={}", orderId, orderSongId);

        // 1. 查询点歌记录
        OrderSong orderSong = orderSongMapper.selectById(orderSongId);
        if (orderSong == null) {
            throw new BusinessException("点歌记录不存在");
        }
        if (!orderSong.getOrderId().equals(orderId)) {
            throw new BusinessException("点歌记录不属于该订单");
        }
        if (orderSong.getStatus() != 0) {
            throw new BusinessException("只能置顶等待中的歌曲");
        }

        // 2. 从Redis队列中移除该歌曲ID
        String queueKey = getQueueKey(orderId);
        stringRedisTemplate.opsForList().remove(queueKey, 1, orderSongId.toString());

        // 3. 将该歌曲ID插入到队列头部
        stringRedisTemplate.opsForList().leftPush(queueKey, orderSongId.toString());

        // 4. 更新数据库中的排序序号（设为0，确保最靠前）
        orderSong.setSortOrder(0);
        orderSongMapper.updateById(orderSong);

        log.info("置顶成功：点歌记录ID={}", orderSongId);
    }

    /**
     * 取消：从队列中移除歌曲
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeSong(Long orderId, Long orderSongId) {
        log.info("取消点歌：订单ID={}, 点歌记录ID={}", orderId, orderSongId);

        // 1. 查询点歌记录
        OrderSong orderSong = orderSongMapper.selectById(orderSongId);
        if (orderSong == null) {
            throw new BusinessException("点歌记录不存在");
        }
        if (!orderSong.getOrderId().equals(orderId)) {
            throw new BusinessException("点歌记录不属于该订单");
        }

        // 2. 从Redis队列中移除该歌曲ID
        String queueKey = getQueueKey(orderId);
        stringRedisTemplate.opsForList().remove(queueKey, 1, orderSongId.toString());

        // 3. 逻辑删除点歌记录（MyBatis-Plus @TableLogic 自动处理）
        orderSongMapper.deleteById(orderSongId);

        log.info("取消点歌成功：点歌记录ID={}", orderSongId);
    }

    /**
     * 查询当前排队列表
     */
    @Override
    public IPage<OrderSong> getQueueList(Page<OrderSong> page, Long orderId) {
        log.info("查询排队列表：订单ID={}", orderId);

        // 查询状态为0（等待中）的点歌记录
        return orderSongMapper.selectByOrderIdAndStatus(page, orderId, 0);
    }

    /**
     * 查询已唱列表
     * Bug9修复：改用 selectPlayedByOrderId，只查 status IN(2,3) 的已播放/已跳过记录
     * 原来传 status=null 会把等待中(0)和播放中(1)的歌也查出来，前端显示错误
     */
    @Override
    public IPage<OrderSong> getPlayedList(Page<OrderSong> page, Long orderId) {
        log.info("查询已唱列表：订单ID={}", orderId);

        // Bug9修复：专门查已播放/已跳过，并按 finish_time 倒序排列
        return orderSongMapper.selectPlayedByOrderId(page, orderId);
    }

    /**
     * 获取Redis队列Key
     */
    private String getQueueKey(Long orderId) {
        return QUEUE_KEY_PREFIX + orderId;
    }
}
