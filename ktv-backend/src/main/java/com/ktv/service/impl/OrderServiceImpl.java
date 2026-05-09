package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ktv.common.enums.OrderStatusEnum;
import com.ktv.common.enums.RoomStatusEnum;
import com.ktv.common.exception.BusinessException;
import com.ktv.constant.RedisKeyConstants;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final long ORDER_KEY_TTL_HOURS = 24;

    private final OrderMapper orderMapper;
    private final RoomMapper roomMapper;
    private final SysUserMapper sysUserMapper;
    private final RoomService roomService;
    private final OrderNoUtil orderNoUtil;
    private final StringRedisTemplate redisTemplate;
    private final RedisLockRegistry redisLockRegistry;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long openOrder(OrderOpenDTO openDTO, Long operatorId) {
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO closeOrder(Long orderId, Long closerId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != OrderStatusEnum.CONSUMING.getCode()) {
            throw new BusinessException("订单状态不允许结账");
        }

        LocalDateTime endTime = LocalDateTime.now();
        long minutes = Duration.between(requireOrderStartTime(order), endTime).toMinutes();
        if (minutes < 1) {
            minutes = 1;
        }

        Room room = lockRoom(order.getRoomId());
        if (room.getPricePerHour() == null) {
            throw new BusinessException("包厢价格未设置");
        }

        BigDecimal roomAmount = calculateRoomAmount(room.getPricePerHour(), minutes);
        BigDecimal minConsumption = room.getMinConsumption() != null
                ? room.getMinConsumption().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = roomAmount.max(minConsumption);

        int updated = orderMapper.atomicCloseOrder(orderId, endTime, (int) minutes, roomAmount, totalAmount, closerId);
        if (updated == 0) {
            throw new BusinessException("订单状态已变更，结账失败");
        }

        roomService.updateRoomStatus(order.getRoomId(), RoomStatusEnum.CLEANING.getCode());
        registerAfterCommit(() -> {
            redisTemplate.delete(RedisKeyConstants.buildCurrentOrderRoomKey(order.getRoomId()));
            clearPlaybackKeys(orderId);
        });

        Order updatedOrder = orderMapper.selectById(orderId);
        log.info("结账成功: orderNo={}, durationMinutes={}, totalAmount={}",
                updatedOrder.getOrderNo(), minutes, totalAmount);
        return convertToVO(updatedOrder);
    }

    @Override
    public IPage<OrderVO> getOrderPage(Page<Order> page, LocalDateTime startDate, LocalDateTime endDate, Long roomId, Integer status) {
        IPage<Order> orderPage = orderMapper.selectOrderPage(page, startDate, endDate, roomId, status);
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
    public Boolean cancelOrder(Long orderId, Long cancellerId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != OrderStatusEnum.CONSUMING.getCode()) {
            throw new BusinessException("只有进行中的订单才能取消");
        }

        LocalDateTime endTime = LocalDateTime.now();
        long minutes = Duration.between(requireOrderStartTime(order), endTime).toMinutes();
        if (minutes < 1) {
            minutes = 1;
        }

        lockRoom(order.getRoomId());
        int updated = orderMapper.atomicCancelOrder(orderId, endTime, (int) minutes, cancellerId);
        if (updated == 0) {
            throw new BusinessException("订单状态已变更，取消失败");
        }

        roomService.updateRoomStatus(order.getRoomId(), RoomStatusEnum.AVAILABLE.getCode());
        registerAfterCommit(() -> {
            redisTemplate.delete(RedisKeyConstants.buildCurrentOrderRoomKey(order.getRoomId()));
            clearPlaybackKeys(orderId);
        });
        log.info("订单已取消: orderNo={}, durationMinutes={}, cancellerId={}",
                order.getOrderNo(), minutes, cancellerId);
        return true;
    }

    @Override
    public OrderVO getActiveOrderByRoomId(Long roomId) {
        Order order = orderMapper.selectActiveOrderByRoomId(roomId);
        return order != null ? convertToVO(order) : null;
    }

    @Override
    public OrderBasicVO getOrderBasicInfo(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("未找到该订单");
        }
        if (!order.isActive()) {
            throw new BusinessException("当前订单未处于进行中状态");
        }

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

    private Long doOpenOrder(OrderOpenDTO openDTO, Long operatorId) {
        Room room = lockRoom(openDTO.getRoomId());
        if (room.getStatus() == null || room.getStatus() != RoomStatusEnum.AVAILABLE.getCode()) {
            throw new BusinessException("包厢当前状态不允许开台，请选择空闲包厢");
        }

        Order activeOrder = orderMapper.selectActiveOrderByRoomId(openDTO.getRoomId());
        if (activeOrder != null) {
            throw new BusinessException("该包厢已有进行中的订单");
        }

        Order order = new Order();
        order.setOrderNo(orderNoUtil.generateOrderNo());
        order.setRoomId(openDTO.getRoomId());
        order.setStartTime(LocalDateTime.now());
        order.setStatus(OrderStatusEnum.CONSUMING.getCode());
        order.setOperatorId(operatorId);
        order.setRemark(openDTO.getRemark());
        order.setRoomAmount(BigDecimal.ZERO);
        order.setTotalAmount(BigDecimal.ZERO);

        int inserted = orderMapper.insert(order);
        if (inserted <= 0 || order.getId() == null) {
            throw new BusinessException("开台失败");
        }

        roomService.updateRoomStatus(openDTO.getRoomId(), RoomStatusEnum.IN_USE.getCode());
        registerAfterCommit(() -> {
            clearPlaybackKeys(order.getId());
            redisTemplate.opsForValue().set(
                    RedisKeyConstants.buildCurrentOrderRoomKey(openDTO.getRoomId()),
                    String.valueOf(order.getId()),
                    ORDER_KEY_TTL_HOURS,
                    TimeUnit.HOURS
            );
        });

        log.info("开台成功: orderNo={}, roomId={}, operatorId={}",
                order.getOrderNo(), openDTO.getRoomId(), operatorId);
        return order.getId();
    }

    private Room lockRoom(Long roomId) {
        Room room = roomMapper.selectByIdForUpdate(roomId);
        if (room == null) {
            throw new BusinessException("包厢不存在");
        }
        return room;
    }

    private BigDecimal calculateRoomAmount(BigDecimal pricePerHour, long minutes) {
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.UP);
        return pricePerHour.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime requireOrderStartTime(Order order) {
        if (order.getStartTime() == null) {
            throw new BusinessException("订单开始时间缺失，无法计算时长");
        }
        return order.getStartTime();
    }

    private void registerAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }

    private void clearPlaybackKeys(Long orderId) {
        try {
            redisTemplate.delete(RedisKeyConstants.buildPlayingKey(orderId));
            redisTemplate.delete(RedisKeyConstants.buildPlayStatusKey(orderId));
            redisTemplate.delete(RedisKeyConstants.buildQueueKey(orderId));
            log.debug("已清理订单 {} 的播放和队列缓存", orderId);
        } catch (Exception e) {
            log.warn("清理订单 {} 的播放缓存失败: {}", orderId, e.getMessage());
        }
    }

    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setStatusText(order.getStatusText());

        if (order.getDurationMinutes() != null && order.getDurationMinutes() > 0) {
            int hours = order.getDurationMinutes() / 60;
            int minutes = order.getDurationMinutes() % 60;
            if (hours > 0 && minutes > 0) {
                vo.setDurationDesc(hours + "小时" + minutes + "分钟");
            } else if (hours > 0) {
                vo.setDurationDesc(hours + "小时");
            } else {
                vo.setDurationDesc(minutes + "分钟");
            }
        }

        if (order.getRoomId() != null && (vo.getRoomName() == null || vo.getRoomType() == null)) {
            Room room = roomMapper.selectById(order.getRoomId());
            if (room != null) {
                if (vo.getRoomName() == null) {
                    vo.setRoomName(room.getName());
                }
                if (vo.getRoomType() == null) {
                    vo.setRoomType(room.getType());
                }
            }
        }

        if (order.getOperatorId() != null && vo.getOperatorName() == null) {
            SysUser operator = sysUserMapper.selectById(order.getOperatorId());
            if (operator != null) {
                vo.setOperatorName(operator.getRealName());
            }
        }

        if (order.getCloserId() != null && vo.getCloserName() == null) {
            SysUser closer = sysUserMapper.selectById(order.getCloserId());
            if (closer != null) {
                vo.setCloserName(closer.getRealName());
            }
        }

        return vo;
    }
}
