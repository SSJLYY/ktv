package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ktv.common.enums.OrderSongStatusEnum;
import com.ktv.common.enums.OrderStatusEnum;
import com.ktv.common.enums.RoomStatusEnum;
import com.ktv.common.exception.BusinessException;
import com.ktv.constant.RedisKeyConstants;
import com.ktv.dto.OrderOpenDTO;
import com.ktv.entity.Order;
import com.ktv.entity.OrderSong;
import com.ktv.entity.Room;
import com.ktv.entity.SysUser;
import com.ktv.mapper.OrderMapper;
import com.ktv.mapper.OrderSongMapper;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final long ORDER_KEY_TTL_HOURS = 24;

    private final OrderMapper orderMapper;
    private final OrderSongMapper orderSongMapper;
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
                throw new BusinessException("Operation busy, please retry");
            }
            try {
                return doOpenOrder(openDTO, operatorId);
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Open order interrupted, please retry");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO closeOrder(Long orderId, Long closerId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("Order not found");
        }
        if (order.getStatus() == null || order.getStatus() != OrderStatusEnum.CONSUMING.getCode()) {
            throw new BusinessException("Order status does not allow closing");
        }

        LocalDateTime endTime = LocalDateTime.now();
        long minutes = Duration.between(requireOrderStartTime(order), endTime).toMinutes();
        if (minutes < 1) {
            minutes = 1;
        }

        Room room = lockRoom(order.getRoomId());
        BigDecimal roomPricePerHour = resolveRoomPricePerHour(order, room);
        BigDecimal minConsumption = resolveRoomMinConsumption(order, room);
        BigDecimal roomAmount = calculateRoomAmount(roomPricePerHour, minutes);
        BigDecimal totalAmount = roomAmount.max(minConsumption);

        int updated = orderMapper.atomicCloseOrder(orderId, endTime, (int) minutes, roomAmount, totalAmount, closerId);
        if (updated == 0) {
            throw new BusinessException("Order status changed, close failed");
        }

        markUnfinishedSongsAsSkipped(orderId, endTime);
        roomService.updateRoomStatus(order.getRoomId(), RoomStatusEnum.CLEANING.getCode());
        registerAfterCommit(() -> {
            clearCurrentOrderRoomKey(order.getRoomId());
            clearPlaybackKeys(orderId);
        });

        Order updatedOrder = orderMapper.selectById(orderId);
        log.info("close order success: orderNo={}, durationMinutes={}, totalAmount={}",
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
            throw new BusinessException("Order not found");
        }
        return convertToVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelOrder(Long orderId, Long cancellerId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("Order not found");
        }
        if (order.getStatus() == null || order.getStatus() != OrderStatusEnum.CONSUMING.getCode()) {
            throw new BusinessException("Only active orders can be cancelled");
        }

        LocalDateTime endTime = LocalDateTime.now();
        long minutes = Duration.between(requireOrderStartTime(order), endTime).toMinutes();
        if (minutes < 1) {
            minutes = 1;
        }

        lockRoom(order.getRoomId());
        int updated = orderMapper.atomicCancelOrder(orderId, endTime, (int) minutes, cancellerId);
        if (updated == 0) {
            throw new BusinessException("Order status changed, cancel failed");
        }

        markUnfinishedSongsAsSkipped(orderId, endTime);
        roomService.updateRoomStatus(order.getRoomId(), RoomStatusEnum.AVAILABLE.getCode());
        registerAfterCommit(() -> {
            clearCurrentOrderRoomKey(order.getRoomId());
            clearPlaybackKeys(orderId);
        });
        log.info("cancel order success: orderNo={}, durationMinutes={}, cancellerId={}",
                order.getOrderNo(), minutes, cancellerId);
        return true;
    }

    @Override
    public OrderVO getActiveOrderByRoomId(Long roomId) {
        Order order = getSingleActiveOrderByRoomId(roomId);
        return order != null ? convertToVO(order) : null;
    }

    @Override
    public OrderBasicVO getActiveOrderBasicByRoomId(Long roomId) {
        Order order = getSingleActiveOrderByRoomId(roomId);
        return order != null ? convertToBasicVO(order) : null;
    }

    @Override
    public OrderBasicVO getOrderBasicInfo(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("Order not found");
        }
        if (!order.isActive()) {
            throw new BusinessException("Current order is not active");
        }
        return convertToBasicVO(order);
    }

    private Long doOpenOrder(OrderOpenDTO openDTO, Long operatorId) {
        Room room = lockRoom(openDTO.getRoomId());
        if (room.getStatus() == null || room.getStatus() != RoomStatusEnum.AVAILABLE.getCode()) {
            throw new BusinessException("Room is not available for opening");
        }

        Order activeOrder = getSingleActiveOrderByRoomId(openDTO.getRoomId());
        if (activeOrder != null) {
            throw new BusinessException("This room already has an active order");
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
        order.setRoomNameSnapshot(room.getName());
        order.setRoomTypeSnapshot(room.getType());
        order.setRoomPricePerHourSnapshot(room.getPricePerHour());
        order.setRoomMinConsumptionSnapshot(room.getMinConsumption());

        int inserted = orderMapper.insert(order);
        if (inserted <= 0 || order.getId() == null) {
            throw new BusinessException("Open order failed");
        }

        roomService.updateRoomStatus(openDTO.getRoomId(), RoomStatusEnum.IN_USE.getCode());
        registerAfterCommit(() -> {
            clearPlaybackKeys(order.getId());
            try {
                redisTemplate.opsForValue().set(
                        RedisKeyConstants.buildCurrentOrderRoomKey(openDTO.getRoomId()),
                        String.valueOf(order.getId()),
                        ORDER_KEY_TTL_HOURS,
                        TimeUnit.HOURS
                );
            } catch (Exception e) {
                log.warn("write current order cache failed for room {}: {}", openDTO.getRoomId(), e.getMessage());
            }
        });

        log.info("open order success: orderNo={}, roomId={}, operatorId={}",
                order.getOrderNo(), openDTO.getRoomId(), operatorId);
        return order.getId();
    }

    private Order getSingleActiveOrderByRoomId(Long roomId) {
        List<Order> activeOrders = orderMapper.selectActiveOrdersByRoomId(roomId);
        if (activeOrders == null || activeOrders.isEmpty()) {
            return null;
        }
        if (activeOrders.size() > 1) {
            log.error("room {} has multiple active orders: {}", roomId,
                    activeOrders.stream().map(Order::getId).toList());
            throw new BusinessException("Multiple active orders found for this room");
        }
        return activeOrders.get(0);
    }

    private Room lockRoom(Long roomId) {
        Room room = roomMapper.selectByIdForUpdate(roomId);
        if (room == null) {
            throw new BusinessException("Room not found");
        }
        return room;
    }

    private BigDecimal calculateRoomAmount(BigDecimal pricePerHour, long minutes) {
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.UP);
        return pricePerHour.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveRoomPricePerHour(Order order, Room currentRoom) {
        BigDecimal snapshotPrice = order.getRoomPricePerHourSnapshot();
        if (snapshotPrice != null) {
            return snapshotPrice.setScale(2, RoundingMode.HALF_UP);
        }
        if (currentRoom != null && currentRoom.getPricePerHour() != null) {
            return currentRoom.getPricePerHour().setScale(2, RoundingMode.HALF_UP);
        }
        throw new BusinessException("Room price is not configured");
    }

    private BigDecimal resolveRoomMinConsumption(Order order, Room currentRoom) {
        BigDecimal snapshotMinConsumption = order.getRoomMinConsumptionSnapshot();
        if (snapshotMinConsumption != null) {
            return snapshotMinConsumption.setScale(2, RoundingMode.HALF_UP);
        }
        if (currentRoom != null && currentRoom.getMinConsumption() != null) {
            return currentRoom.getMinConsumption().setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime requireOrderStartTime(Order order) {
        if (order.getStartTime() == null) {
            throw new BusinessException("Order start time is missing");
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

    private void markUnfinishedSongsAsSkipped(Long orderId, LocalDateTime finishTime) {
        LambdaUpdateWrapper<OrderSong> updateWrapper = new LambdaUpdateWrapper<OrderSong>()
                .eq(OrderSong::getOrderId, orderId)
                .eq(OrderSong::getDeleted, 0)
                .in(OrderSong::getStatus,
                        OrderSongStatusEnum.WAITING.getCode(),
                        OrderSongStatusEnum.PLAYING.getCode())
                .set(OrderSong::getStatus, OrderSongStatusEnum.SKIPPED.getCode())
                .set(OrderSong::getFinishTime, finishTime)
                .set(OrderSong::getUpdateTime, finishTime);
        orderSongMapper.update(null, updateWrapper);
    }

    private void clearCurrentOrderRoomKey(Long roomId) {
        try {
            redisTemplate.delete(RedisKeyConstants.buildCurrentOrderRoomKey(roomId));
        } catch (Exception e) {
            log.warn("clear current order cache failed for room {}: {}", roomId, e.getMessage());
        }
    }

    private void clearPlaybackKeys(Long orderId) {
        try {
            redisTemplate.delete(RedisKeyConstants.buildPlayingKey(orderId));
            redisTemplate.delete(RedisKeyConstants.buildPlayStatusKey(orderId));
            redisTemplate.delete(RedisKeyConstants.buildQueueKey(orderId));
            log.debug("cleared playback cache for order {}", orderId);
        } catch (Exception e) {
            log.warn("clear playback cache failed for order {}: {}", orderId, e.getMessage());
        }
    }

    private OrderBasicVO convertToBasicVO(Order order) {
        Room room = null;
        if (order.getRoomId() != null && order.getRoomNameSnapshot() == null) {
            room = roomMapper.selectById(order.getRoomId());
        }
        String roomName = order.getRoomNameSnapshot();
        if (roomName == null && room != null) {
            roomName = room.getName();
        }

        return OrderBasicVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .roomId(order.getRoomId())
                .status(order.getStatus())
                .statusText(order.getStatusText())
                .roomName(roomName)
                .build();
    }

    private OrderVO convertToVO(Order order) {
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);
        vo.setStatusText(order.getStatusText());
        Room room = null;
        if (order.getRoomId() != null
                && (order.getRoomNameSnapshot() == null || order.getRoomTypeSnapshot() == null)) {
            room = roomMapper.selectById(order.getRoomId());
        }
        vo.setRoomName(order.getRoomNameSnapshot() != null
                ? order.getRoomNameSnapshot()
                : room != null ? room.getName() : null);
        vo.setRoomType(order.getRoomTypeSnapshot() != null
                ? order.getRoomTypeSnapshot()
                : room != null ? room.getType() : null);

        if (order.getDurationMinutes() != null && order.getDurationMinutes() > 0) {
            int hours = order.getDurationMinutes() / 60;
            int minutes = order.getDurationMinutes() % 60;
            if (hours > 0 && minutes > 0) {
                vo.setDurationDesc(hours + "h" + minutes + "m");
            } else if (hours > 0) {
                vo.setDurationDesc(hours + "h");
            } else {
                vo.setDurationDesc(minutes + "m");
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
