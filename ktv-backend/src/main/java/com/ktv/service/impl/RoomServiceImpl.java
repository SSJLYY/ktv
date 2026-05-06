package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktv.common.enums.RoomStatusEnum;
import com.ktv.common.exception.BusinessException;
import com.ktv.constant.RedisKeyConstants;
import com.ktv.dto.RoomDTO;
import com.ktv.entity.Room;
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

/**
 * 包厢服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements RoomService {

    private final RoomMapper roomMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public List<RoomVO> getRoomList(Integer status, String type) {
        LambdaQueryWrapper<Room> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            queryWrapper.eq(Room::getStatus, status);
        }
        if (type != null && !type.isEmpty()) {
            queryWrapper.eq(Room::getType, type);
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
        assertRoomNameUnique(roomDTO.getName(), null);

        Room room = new Room();
        BeanUtils.copyProperties(roomDTO, room);
        if (room.getStatus() == null) {
            room.setStatus(RoomStatusEnum.AVAILABLE.getCode());
        }
        if (room.getMinConsumption() == null) {
            room.setMinConsumption(BigDecimal.ZERO);
        }

        roomMapper.insert(room);
        registerAfterCommit(() -> syncRoomStatusToRedis(loadRoom(room.getId())));
        return room.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateRoom(Long id, RoomDTO roomDTO) {
        Room existRoom = loadRoom(id);
        String targetName = roomDTO.getName() != null ? roomDTO.getName() : existRoom.getName();
        assertRoomNameUnique(targetName, id);

        Room room = new Room();
        BeanUtils.copyProperties(roomDTO, room);
        room.setId(id);
        if (room.getStatus() == null) {
            room.setStatus(existRoom.getStatus());
        }
        if (room.getMinConsumption() == null) {
            room.setMinConsumption(existRoom.getMinConsumption());
        }

        boolean changed = hasRoomCacheRelevantChange(existRoom, roomDTO, room);
        boolean updated = roomMapper.updateById(room) > 0;
        if (updated && changed) {
            registerAfterCommit(() -> syncRoomStatusToRedis(loadRoom(id)));
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteRoom(Long id) {
        Room existRoom = loadRoom(id);
        if (existRoom.getStatus() == null || existRoom.getStatus() != RoomStatusEnum.AVAILABLE.getCode()) {
            throw new BusinessException("仅允许删除状态为\"空闲\"的包厢");
        }

        boolean deleted = roomMapper.deleteById(id) > 0;
        if (deleted) {
            registerAfterCommit(() -> removeRoomStatusFromRedis(id));
        }
        return deleted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateRoomStatus(Long id, Integer status) {
        Room existRoom = loadRoom(id);
        if (status == null
                || status < RoomStatusEnum.AVAILABLE.getCode()
                || status > RoomStatusEnum.MAINTENANCE.getCode()) {
            throw new BusinessException("无效的状态值");
        }

        Room room = new Room();
        room.setId(id);
        room.setStatus(status);
        boolean updated = roomMapper.updateById(room) > 0;
        if (updated && !Objects.equals(existRoom.getStatus(), status)) {
            registerAfterCommit(() -> syncRoomStatusToRedis(loadRoom(id)));
        }
        return updated;
    }

    @Override
    public RoomVO getRoomById(Long id) {
        return convertToVO(loadRoom(id));
    }

    @Override
    public void syncRoomStatusToRedis(Room room) {
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
            log.warn("同步包厢状态到 Redis 失败（序列化错误）: {}", e.getMessage());
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

    private boolean hasRoomCacheRelevantChange(Room existRoom, RoomDTO roomDTO, Room updatedRoom) {
        String newName = roomDTO.getName() != null ? roomDTO.getName() : existRoom.getName();
        String newType = roomDTO.getType() != null ? roomDTO.getType() : existRoom.getType();
        return !Objects.equals(existRoom.getName(), newName)
                || !Objects.equals(existRoom.getType(), newType)
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
