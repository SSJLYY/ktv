package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktv.common.enums.RoomStatusEnum;
import com.ktv.common.exception.BusinessException;
import com.ktv.constant.RedisKeyConstants;
import com.ktv.dto.RoomDTO;
import com.ktv.entity.Order;
import com.ktv.entity.Room;
import com.ktv.mapper.OrderMapper;
import com.ktv.mapper.RoomMapper;
import com.ktv.service.RoomService;
import com.ktv.vo.RoomVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements RoomService {

    private final RoomMapper roomMapper;
    private final OrderMapper orderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<RoomVO> getRoomList(Integer status, String type) {
        LambdaQueryWrapper<Room> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            queryWrapper.eq(Room::getStatus, status);
        }
        if (type != null && !type.trim().isEmpty()) {
            queryWrapper.eq(Room::getType, type.trim());
        }
        queryWrapper.orderByAsc(Room::getName);
        return roomMapper.selectList(queryWrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoomVO> getAvailableRooms() {
        LambdaQueryWrapper<Room> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Room::getStatus, RoomStatusEnum.AVAILABLE.getCode());
        queryWrapper.orderByAsc(Room::getName);
        return roomMapper.selectList(queryWrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRoom(RoomDTO roomDTO) {
        String normalizedName = normalizeRequiredText(roomDTO.getName(), "包厢名称不能为空");
        String normalizedType = normalizeRequiredText(roomDTO.getType(), "包厢类型不能为空");
        assertRoomNameUnique(normalizedName, null);

        Room room = new Room();
        BeanUtils.copyProperties(roomDTO, room);
        room.setName(normalizedName);
        room.setType(normalizedType);
        if (room.getStatus() == null) {
            room.setStatus(RoomStatusEnum.AVAILABLE.getCode());
        }
        if (room.getMinConsumption() == null) {
            room.setMinConsumption(BigDecimal.ZERO);
        }
        normalizeOptionalDescription(room);
        validateRoomBusinessFields(room);

        int inserted = roomMapper.insert(room);
        if (inserted <= 0 || room.getId() == null) {
            throw new BusinessException("新增包厢失败");
        }

        registerAfterCommit(() -> syncRoomStatusToRedis(loadRoom(room.getId())));
        return room.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateRoom(Long id, RoomDTO roomDTO) {
        Room existRoom = lockRoom(id);
        String targetName = roomDTO.getName() != null
                ? normalizeRequiredText(roomDTO.getName(), "包厢名称不能为空")
                : existRoom.getName();
        assertRoomNameUnique(targetName, id);

        Room room = new Room();
        BeanUtils.copyProperties(roomDTO, room);
        room.setId(id);
        room.setName(targetName);
        room.setType(roomDTO.getType() != null
                ? normalizeRequiredText(roomDTO.getType(), "包厢类型不能为空")
                : existRoom.getType());
        if (room.getCapacity() == null) {
            room.setCapacity(existRoom.getCapacity());
        }
        if (room.getPricePerHour() == null) {
            room.setPricePerHour(existRoom.getPricePerHour());
        }
        room.setStatus(existRoom.getStatus());
        if (room.getMinConsumption() == null) {
            room.setMinConsumption(existRoom.getMinConsumption());
        }
        if (roomDTO.getDescription() == null) {
            room.setDescription(existRoom.getDescription());
        }
        normalizeOptionalDescription(room);
        validateRoomBusinessFields(room);

        boolean changed = hasRoomCacheRelevantChange(existRoom, room);
        boolean updated = roomMapper.updateById(room) > 0;
        if (!updated) {
            throw new BusinessException("修改包厢失败");
        }
        if (changed) {
            registerAfterCommit(() -> syncRoomStatusToRedis(loadRoom(id)));
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteRoom(Long id) {
        Room existRoom = lockRoom(id);
        if (existRoom.getStatus() == null || existRoom.getStatus() != RoomStatusEnum.AVAILABLE.getCode()) {
            throw new BusinessException("仅允许删除状态为空闲的包厢");
        }
        Long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getRoomId, id));
        if (orderCount != null && orderCount > 0) {
            throw new BusinessException("该包厢已存在订单记录，无法删除");
        }

        boolean deleted = roomMapper.deleteById(id) > 0;
        if (!deleted) {
            throw new BusinessException("删除包厢失败");
        }

        registerAfterCommit(() -> removeRoomStatusFromRedis(id));
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateRoomStatus(Long id, Integer status) {
        Room existRoom = lockRoom(id);
        if (status == null
                || status < RoomStatusEnum.AVAILABLE.getCode()
                || status > RoomStatusEnum.MAINTENANCE.getCode()) {
            throw new BusinessException("无效的包厢状态");
        }
        if (Objects.equals(existRoom.getStatus(), status)) {
            return true;
        }

        Order activeOrder = orderMapper.selectActiveOrderByRoomId(id);
        if (activeOrder != null && status != RoomStatusEnum.IN_USE.getCode()) {
            throw new BusinessException("该包厢存在进行中的订单，不能改为非使用中状态");
        }
        if (activeOrder == null && status == RoomStatusEnum.IN_USE.getCode()) {
            throw new BusinessException("该包厢没有进行中的订单，不能直接改为使用中");
        }

        Room room = new Room();
        room.setId(id);
        room.setStatus(status);
        boolean updated = roomMapper.updateById(room) > 0;
        if (!updated) {
            throw new BusinessException("更新包厢状态失败");
        }

        registerAfterCommit(() -> syncRoomStatusToRedis(loadRoom(id)));
        return true;
    }

    @Override
    public RoomVO getRoomById(Long id) {
        return convertToVO(loadRoom(id));
    }

    @Override
    public void syncRoomStatusToRedis(Room room) {
        if (room == null || room.getId() == null) {
            return;
        }
        try {
            Map<String, Object> roomStatus = new HashMap<>();
            roomStatus.put("id", room.getId());
            roomStatus.put("name", room.getName());
            roomStatus.put("type", room.getType());
            roomStatus.put("status", room.getStatus());
            roomStatus.put("statusText", room.getStatusText());

            String json = objectMapper.writeValueAsString(roomStatus);
            stringRedisTemplate.opsForHash().put(RedisKeyConstants.ROOM_STATUS, room.getId().toString(), json);
            log.debug("包厢状态已同步到 Redis: id={}, status={}", room.getId(), room.getStatus());
        } catch (JsonProcessingException e) {
            log.warn("包厢状态序列化失败: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("同步包厢状态到 Redis 失败: {}", e.getMessage());
        }
    }

    private Room loadRoom(Long id) {
        Room room = roomMapper.selectById(id);
        if (room == null) {
            throw new BusinessException("包厢不存在");
        }
        return room;
    }

    private Room lockRoom(Long id) {
        Room room = roomMapper.selectByIdForUpdate(id);
        if (room == null) {
            throw new BusinessException("包厢不存在");
        }
        return room;
    }

    private void assertRoomNameUnique(String roomName, Long excludeId) {
        LambdaQueryWrapper<Room> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Room::getName, roomName);
        if (excludeId != null) {
            queryWrapper.ne(Room::getId, excludeId);
        }
        Long count = roomMapper.selectCount(queryWrapper);
        if (count != null && count > 0) {
            throw new BusinessException("包厢名称已存在");
        }
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null) {
            throw new BusinessException(message);
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(message);
        }
        return normalized;
    }

    private void normalizeOptionalDescription(Room room) {
        if (room.getDescription() != null) {
            room.setDescription(room.getDescription().trim());
        }
    }

    private void validateRoomBusinessFields(Room room) {
        if (room.getCapacity() == null || room.getCapacity() <= 0) {
            throw new BusinessException("包厢容纳人数必须大于 0");
        }
        if (room.getPricePerHour() == null || room.getPricePerHour().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("每小时价格不能为负数");
        }
        if (room.getMinConsumption() != null && room.getMinConsumption().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("最低消费不能为负数");
        }
    }

    private boolean hasRoomCacheRelevantChange(Room existRoom, Room updatedRoom) {
        return !Objects.equals(existRoom.getName(), updatedRoom.getName())
                || !Objects.equals(existRoom.getType(), updatedRoom.getType())
                || !Objects.equals(existRoom.getStatus(), updatedRoom.getStatus());
    }

    private void removeRoomStatusFromRedis(Long id) {
        try {
            stringRedisTemplate.opsForHash().delete(RedisKeyConstants.ROOM_STATUS, id.toString());
        } catch (Exception e) {
            log.warn("从 Redis 删除包厢状态失败: {}", e.getMessage());
        }
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

    private RoomVO convertToVO(Room room) {
        RoomVO roomVO = new RoomVO();
        BeanUtils.copyProperties(room, roomVO);
        roomVO.setStatusText(room.getStatusText());
        return roomVO;
    }
}
