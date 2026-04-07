package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ktv.common.enums.RoomStatusEnum;
import com.ktv.common.enums.OrderStatusEnum;
import com.ktv.common.exception.BusinessException;
import com.ktv.dto.OrderOpenDTO;
import com.ktv.entity.Order;
import com.ktv.entity.Room;
import com.ktv.entity.SysUser;
import com.ktv.mapper.OrderMapper;
import com.ktv.mapper.RoomMapper;
import com.ktv.mapper.SysUserMapper;
import com.ktv.service.OrderService;
import com.ktv.service.RoomService;
import com.ktv.util.OrderNoUtil;
import com.ktv.vo.OrderBasicVO;
import com.ktv.vo.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * 订单Service实现
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderMapper orderMapper;
    private final RoomMapper roomMapper;
    private final SysUserMapper sysUserMapper;
    private final RoomService roomService;
    private final OrderNoUtil orderNoUtil;
    /**
     * Bug6修复：改用 StringRedisTemplate，与 PlayQueueServiceImpl / PlayControlServiceImpl 保持一致，
     * 避免 RedisTemplate<String,Object> 的 Jackson 序列化与 StringRedisTemplate 的字符串值不兼容
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * S11修复：Redis 分布式锁注册中心，防止并发开台竞态
     * 按包厢维度加锁，key: "lock:open_order:room:{roomId}"
     */
    private final RedisLockRegistry redisLockRegistry;

    /**
     * Redis Key前缀：点歌队列（与 PlayQueueServiceImpl 保持一致：ktv:queue:{orderId}）
     * BugD修复：原来错误地使用了 "ktv:queue:room:{roomId}"，
     * 实际点歌队列 Key 是 "ktv:queue:{orderId}"，两者不一致导致开台时无法清空队列
     */
    private static final String REDIS_QUEUE_KEY_PREFIX = "ktv:queue:";

    /**
     * Redis Key：当前订单（包厢维度）
     */
    private static final String REDIS_CURRENT_ORDER_KEY = "ktv:current_order:room:";

    /**
     * Redis Key：当前播放歌曲（与 PlayControlServiceImpl 保持一致：ktv:playing:{orderId}）
     * Bug7修复：结账/取消时需要清理这些 key
     */
    private static final String REDIS_PLAYING_KEY_PREFIX = "ktv:playing:";

    /**
     * Redis Key：播放状态（与 PlayControlServiceImpl 保持一致：ktv:play:status:{orderId}）
     */
    private static final String REDIS_PLAY_STATUS_KEY_PREFIX = "ktv:play:status:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long openOrder(OrderOpenDTO openDTO, Long operatorId) {
        // S11修复：使用 Redis 分布式锁，防止同一包厢被并发开台
        String lockKey = "lock:open_order:room:" + openDTO.getRoomId();
        Lock lock = redisLockRegistry.obtain(lockKey);
        try {
            if (!lock.tryLock(10, TimeUnit.SECONDS)) {
                throw new BusinessException("操作繁忙，请稍后重试");
            }
            try {
                return doOpenOrder(openDTO, operatorId);
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("开台操作被中断，请重试");
        }
    }

    /**
     * 开台实际逻辑（S11修复：从 openOrder 抽取，在分布式锁内执行）
     */
    private Long doOpenOrder(OrderOpenDTO openDTO, Long operatorId) {
        // 1. 检查包厢是否存在
        Room room = roomMapper.selectById(openDTO.getRoomId());
        if (room == null) {
            throw new BusinessException("包厢不存在");
        }

        // 2. H1修复：检查包厢状态是否为"空闲"，防止NPE
        Integer roomStatus = room.getStatus();
        if (roomStatus == null || roomStatus != RoomStatusEnum.AVAILABLE.getCode()) {
            throw new BusinessException("包厢当前状态不允许开台，请选择空闲包厢");
        }

        // 3. 检查该包厢是否已有进行中的订单
        Order activeOrder = orderMapper.selectActiveOrderByRoomId(openDTO.getRoomId());
        if (activeOrder != null) {
            throw new BusinessException("该包厢已有进行中的订单");
        }

        // 4. 生成订单编号
        String orderNo = orderNoUtil.generateOrderNo();

        // 5. 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setRoomId(openDTO.getRoomId());
        order.setStartTime(LocalDateTime.now());
        order.setStatus(OrderStatusEnum.CONSUMING.getCode()); // 消费中
        order.setOperatorId(operatorId);
        order.setRemark(openDTO.getRemark());
        order.setRoomAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);

        orderMapper.insert(order);

        // 6. 更新包厢状态为"使用中"
        roomService.updateRoomStatus(openDTO.getRoomId(), RoomStatusEnum.IN_USE.getCode());

        // 7. Bug7修复：清空该订单相关的所有Redis播放状态key
        // 包括：队列 ktv:queue:{orderId}、当前播放 ktv:playing:{orderId}、播放状态 ktv:play:status:{orderId}
        // 注：此时 order.getId() 是新订单ID，理论上不存在旧数据；但防御性清理保证干净状态
        clearPlaybackKeys(order.getId());

        // 8. 记录当前订单到Redis（方便包厢点歌端查询）
        // Bug6修复：StringRedisTemplate 只能存 String，orderId 需转为字符串
        redisTemplate.opsForValue().set(REDIS_CURRENT_ORDER_KEY + openDTO.getRoomId(), order.getId().toString(), 24, TimeUnit.HOURS);

        log.info("开台成功：订单号={}, 包厢ID={}, 操作员ID={}", orderNo, openDTO.getRoomId(), operatorId);

        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO closeOrder(Long orderId, Long closerId) {
        // 1. 检查订单是否存在
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 2. H2修复：检查订单状态是否为"消费中"，防止NPE
        Integer orderStatus = order.getStatus();
        if (orderStatus == null || orderStatus != OrderStatusEnum.CONSUMING.getCode()) {
            throw new BusinessException("订单状态不允许结账");
        }

        // C-1修复：使用原子更新防止并发结账竞态
        // 先计算费用，再用 updateOrderStatusWithCondition 原子更新（WHERE id=? AND status=1）
        LocalDateTime endTime = LocalDateTime.now();
        long minutes = Duration.between(order.getStartTime(), endTime).toMinutes();
        if (minutes < 1) {
            minutes = 1;
        }

        Room room = roomMapper.selectById(order.getRoomId());
        if (room == null) {
            throw new BusinessException("包厢不存在");
        }

        BigDecimal pricePerHour = room.getPricePerHour();
        if (pricePerHour == null) {
            throw new BusinessException("包厢价格未设置");
        }
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.UP);
        BigDecimal roomAmount = pricePerHour.multiply(hours).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = roomAmount;

        // C-1修复：原子更新，只有status=1的订单才能被结账
        int updated = orderMapper.atomicCloseOrder(order.getId(), endTime, (int) minutes, roomAmount, totalAmount, closerId);
        if (updated == 0) {
            throw new BusinessException("订单状态已变更，结账失败");
        }

        // 更新包厢状态为"清洁中"
        roomService.updateRoomStatus(order.getRoomId(), RoomStatusEnum.CLEANING.getCode());

        // 9. 清理点歌队列
        try {
            String queueKey = "ktv:queue:" + orderId;
            Long queueSize = redisTemplate.opsForList().size(queueKey);
            if (queueSize != null && queueSize > 0) {
                redisTemplate.delete(queueKey);
                log.info("结账时已清理订单{}的点歌队列，共{}首歌曲", orderId, queueSize);
            }
        } catch (Exception e) {
            log.warn("清理点歌队列失败（不影响结账）: {}", e.getMessage());
        }

        // 10. 清除Redis中的当前订单记录 + 播放相关状态
        redisTemplate.delete(REDIS_CURRENT_ORDER_KEY + order.getRoomId());
        clearPlaybackKeys(orderId);

        // 11. 重新查询订单获取最新数据（原子更新后 order 对象已过时）
        Order updatedOrder = orderMapper.selectById(orderId);

        log.info("结账成功：订单号={}, 时长={}分钟, 费用={}元", updatedOrder.getOrderNo(), minutes, totalAmount);

        return convertToVO(updatedOrder);
    }

    @Override
    public IPage<OrderVO> getOrderPage(
            Page<Order> page,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long roomId,
            Integer status
    ) {
        IPage<Order> orderPage = orderMapper.selectOrderPage(page, startDate, endDate, roomId, status);
        
        // 转换为VO
        IPage<OrderVO> voPage = new Page<>(orderPage.getCurrent(), orderPage.getSize(), orderPage.getTotal());
        voPage.setRecords(orderPage.getRecords().stream().map(this::convertToVO).toList());
        
        return voPage;
    }

    @Override
    public OrderVO getOrderById(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return convertToVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // H4修复：检查订单状态，防止NPE
        Integer orderStatus = order.getStatus();
        if (orderStatus == null || orderStatus != OrderStatusEnum.CONSUMING.getCode()) {
            throw new BusinessException("只有消费中的订单才能取消");
        }

        // 更新状态为已取消（使用原子更新防止并发取消竞态）
        int updated = orderMapper.atomicCancelOrder(orderId);

        if (updated > 0) {
            // 将包厢状态恢复为"空闲"
            roomService.updateRoomStatus(order.getRoomId(), RoomStatusEnum.AVAILABLE.getCode());
            
            // 清除Redis记录（含播放状态）
            // Bug7修复：同时清理播放状态 key，防止旧状态残留
            redisTemplate.delete(REDIS_CURRENT_ORDER_KEY + order.getRoomId());
            clearPlaybackKeys(orderId);
            
            log.info("订单已取消：订单号={}", order.getOrderNo());
        }

        return updated > 0;
    }

    @Override
    public OrderVO getActiveOrderByRoomId(Long roomId) {
        Order order = orderMapper.selectActiveOrderByRoomId(roomId);
        return order != null ? convertToVO(order) : null;
    }

    /**
     * S6/S7修复：获取订单基础信息（包厢端加入验证用）
     * S7修复：返回强类型 OrderBasicVO 替代 Map<String,Object>
     */
    @Override
    public OrderBasicVO getOrderBasicInfo(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("未找到该订单");
        }

        // 查询包厢名称
        String roomName = null;
        if (order.getRoomId() != null) {
            Room room = roomMapper.selectById(order.getRoomId());
            if (room != null) {
                roomName = room.getName();
            }
        }

        return OrderBasicVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .status(order.getStatus())
                .statusText(order.getStatusText())
                .roomName(roomName)
                .build();
    }

    /**
     * 清理指定订单的播放相关Redis key（当前播放歌曲 + 播放状态）
     * Bug7修复：结账/取消时调用，防止旧播放状态残留影响下一个订单
     */
    private void clearPlaybackKeys(Long orderId) {
        try {
            redisTemplate.delete(REDIS_PLAYING_KEY_PREFIX + orderId);
            redisTemplate.delete(REDIS_PLAY_STATUS_KEY_PREFIX + orderId);
            redisTemplate.delete(REDIS_QUEUE_KEY_PREFIX + orderId);
            log.debug("已清理订单{}的播放状态Redis key", orderId);
        } catch (Exception e) {
            log.warn("清理播放状态Redis key失败: {}", e.getMessage());
            // 不影响主流程
        }
    }

    /**
     * 将Order实体转换为OrderVO
     */
    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        
        // 设置状态文本
        vo.setStatusText(order.getStatusText());

        // 设置消费时长描述
        if (order.getDurationMinutes() != null && order.getDurationMinutes() > 0) {
            int h = order.getDurationMinutes() / 60;
            int m = order.getDurationMinutes() % 60;
            if (h > 0 && m > 0) {
                vo.setDurationDesc(h + "小时" + m + "分钟");
            } else if (h > 0) {
                vo.setDurationDesc(h + "小时");
            } else {
                vo.setDurationDesc(m + "分钟");
            }
        }
        
        // Bug20修复（N+1 优化）：Order 实体的 roomName/operatorName 可能已由 selectOrderPage 的 JOIN 填充
        // 只在 roomName 为 null 时才补查，避免 getOrderPage 场景下发生 N+1 重复查询
        if (order.getRoomId() != null && (vo.getRoomName() == null || vo.getRoomType() == null)) {
            Room room = roomMapper.selectById(order.getRoomId());
            if (room != null) {
                if (vo.getRoomName() == null) vo.setRoomName(room.getName());
                if (vo.getRoomType() == null) vo.setRoomType(room.getType());
            }
        }

        // operatorName 已由 ORDER_PAGE JOIN 填充，同样只在 null 时补查
        if (order.getOperatorId() != null && vo.getOperatorName() == null) {
            SysUser operator = sysUserMapper.selectById(order.getOperatorId());
            if (operator != null) {
                vo.setOperatorName(operator.getRealName());
            }
        }

        // S2修复：closerName 同样由分页 JOIN 填充，只在 null 时补查，避免 N+1 重复查询
        if (order.getCloserId() != null && vo.getCloserName() == null) {
            SysUser closer = sysUserMapper.selectById(order.getCloserId());
            if (closer != null) {
                vo.setCloserName(closer.getRealName());
            }
        }
        
        return vo;
    }
}
