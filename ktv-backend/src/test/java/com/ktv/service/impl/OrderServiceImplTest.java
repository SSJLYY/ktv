package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.ktv.common.enums.OrderStatusEnum;
import com.ktv.common.enums.RoomStatusEnum;
import com.ktv.dto.OrderOpenDTO;
import com.ktv.entity.Order;
import com.ktv.entity.OrderSong;
import com.ktv.entity.Room;
import com.ktv.mapper.OrderMapper;
import com.ktv.mapper.OrderSongMapper;
import com.ktv.mapper.RoomMapper;
import com.ktv.mapper.SysUserMapper;
import com.ktv.service.RoomService;
import com.ktv.util.OrderNoUtil;
import com.ktv.vo.OrderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.integration.redis.util.RedisLockRegistry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderSongMapper orderSongMapper;
    @Mock
    private RoomMapper roomMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private RoomService roomService;
    @Mock
    private OrderNoUtil orderNoUtil;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private RedisLockRegistry redisLockRegistry;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private OrderSongMapper deepStubOrderSongMapper;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() throws InterruptedException {
        var tableInfo = TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new MybatisConfiguration(), ""),
                OrderSong.class
        );
        LambdaUtils.installCache(tableInfo);

        orderService = new OrderServiceImpl(
                orderMapper,
                deepStubOrderSongMapper,
                roomMapper,
                sysUserMapper,
                roomService,
                orderNoUtil,
                redisTemplate,
                redisLockRegistry
        );
    }

    @Test
    void openOrderShouldFreezeRoomSnapshot() throws InterruptedException {
        Long roomId = 8L;
        Room room = buildRoom(roomId, "A01", "SMALL", "88.00", "20.00", RoomStatusEnum.AVAILABLE.getCode());
        OrderOpenDTO openDTO = new OrderOpenDTO();
        openDTO.setRoomId(roomId);
        openDTO.setRemark("snapshot");

        when(roomMapper.selectByIdForUpdate(roomId)).thenReturn(room);
        when(orderMapper.selectActiveOrdersByRoomId(roomId)).thenReturn(Collections.emptyList());
        when(orderNoUtil.generateOrderNo()).thenReturn("KTV202605160001");
        Lock lock = mock(Lock.class);
        when(redisLockRegistry.obtain(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), eq(TimeUnit.SECONDS))).thenReturn(true);
        when(redisTemplate.delete(anyString())).thenReturn(Boolean.TRUE);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(101L);
            return 1;
        }).when(orderMapper).insert(any(Order.class));

        Long orderId = orderService.openOrder(openDTO, 66L);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).insert(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertEquals(101L, orderId);
        assertEquals("A01", savedOrder.getRoomNameSnapshot());
        assertEquals("SMALL", savedOrder.getRoomTypeSnapshot());
        assertEquals(0, new BigDecimal("88.00").compareTo(savedOrder.getRoomPricePerHourSnapshot()));
        assertEquals(0, new BigDecimal("20.00").compareTo(savedOrder.getRoomMinConsumptionSnapshot()));
    }

    @Test
    void closeOrderShouldUseFrozenRoomPriceWhenCurrentRoomPriceChanged() throws InterruptedException {
        Long orderId = 12L;
        Long roomId = 3L;
        Order order = new Order();
        order.setId(orderId);
        order.setOrderNo("KTV202605160002");
        order.setRoomId(roomId);
        order.setStatus(OrderStatusEnum.CONSUMING.getCode());
        order.setStartTime(LocalDateTime.now().minusMinutes(60));
        order.setRoomPricePerHourSnapshot(new BigDecimal("88.00"));
        order.setRoomMinConsumptionSnapshot(BigDecimal.ZERO);

        Room currentRoom = buildRoom(roomId, "B02-new", "LARGE", "188.00", "0.00", RoomStatusEnum.IN_USE.getCode());

        when(orderMapper.selectById(orderId)).thenReturn(order);
        when(roomMapper.selectByIdForUpdate(roomId)).thenReturn(currentRoom);
        when(redisTemplate.delete(anyString())).thenReturn(Boolean.TRUE);
        when(orderMapper.atomicCloseOrder(
                eq(orderId),
                any(LocalDateTime.class),
                anyInt(),
                any(BigDecimal.class),
                any(BigDecimal.class),
                eq(99L)
        )).thenReturn(1);

        orderService.closeOrder(orderId, 99L);

        ArgumentCaptor<BigDecimal> roomAmountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> totalAmountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(orderMapper).atomicCloseOrder(
                eq(orderId),
                any(LocalDateTime.class),
                anyInt(),
                roomAmountCaptor.capture(),
                totalAmountCaptor.capture(),
                eq(99L)
        );
        assertEquals(0, new BigDecimal("88.00").compareTo(roomAmountCaptor.getValue()));
        assertEquals(0, new BigDecimal("88.00").compareTo(totalAmountCaptor.getValue()));
    }

    @Test
    void getOrderByIdShouldPreferFrozenRoomSnapshot() {
        Order order = new Order();
        order.setId(5L);
        order.setOrderNo("KTV202605160003");
        order.setRoomId(18L);
        order.setStatus(OrderStatusEnum.CONSUMING.getCode());
        order.setRoomNameSnapshot("VIP-9");
        order.setRoomTypeSnapshot("VIP");

        when(orderMapper.selectById(5L)).thenReturn(order);

        OrderVO result = orderService.getOrderById(5L);

        assertEquals("VIP-9", result.getRoomName());
        assertEquals("VIP", result.getRoomType());
        verify(roomMapper, never()).selectById(anyLong());
    }

    @Test
    void getOrderByIdShouldFallbackToCurrentRoomForLegacyOrder() {
        Order order = new Order();
        order.setId(6L);
        order.setOrderNo("KTV202605160004");
        order.setRoomId(28L);
        order.setStatus(OrderStatusEnum.CONSUMING.getCode());

        Room currentRoom = buildRoom(28L, "Legacy-01", "SMALL", "68.00", "0.00", RoomStatusEnum.IN_USE.getCode());

        when(orderMapper.selectById(6L)).thenReturn(order);
        when(roomMapper.selectById(28L)).thenReturn(currentRoom);

        OrderVO result = orderService.getOrderById(6L);

        assertEquals("Legacy-01", result.getRoomName());
        assertEquals("SMALL", result.getRoomType());
        assertNull(result.getOperatorName());
    }

    private Room buildRoom(Long id, String name, String type, String pricePerHour, String minConsumption, Integer status) {
        Room room = new Room();
        room.setId(id);
        room.setName(name);
        room.setType(type);
        room.setPricePerHour(new BigDecimal(pricePerHour));
        room.setMinConsumption(new BigDecimal(minConsumption));
        room.setStatus(status);
        room.setCapacity(6);
        return room;
    }
}
