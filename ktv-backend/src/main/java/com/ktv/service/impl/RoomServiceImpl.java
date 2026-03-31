package com.ktv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktv.common.exception.BusinessException;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 包厢Service实现
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room> implements RoomService {

    private final RoomMapper roomMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis Key前缀：包厢状态快照
     */
    private static final String REDIS_ROOM_STATUS_KEY = "ktv:room:status";

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
        List<Room> roomList = roomMapper.selectList(queryWrapper);
        return roomList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RoomVO> getAvailableRooms() {
        LambdaQueryWrapper<Room> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Room::getStatus, 0); // 0=空闲
        queryWrapper.orderByAsc(Room::getName);
        List<Room> roomList = roomMapper.selectList(queryWrapper);
        return roomList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRoom(RoomDTO roomDTO) {
        // 检查包厢名是否已存在
        LambdaQueryWrapper<Room> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Room::getName, roomDTO.getName());
        Long count = roomMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException("包厢名称已存在");
        }

        Room room = new Room();
        BeanUtils.copyProperties(roomDTO, room);

        // 设置默认值
        if (room.getStatus() == null) {
            room.setStatus(0); // 默认空闲
        }
        if (room.getMinConsumption() == null) {
            room.setMinConsumption(java.math.BigDecimal.ZERO);
        }

        roomMapper.insert(room);

        // 同步状态到Redis
        syncRoomStatusToRedis(room);

        return room.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateRoom(Long id, RoomDTO roomDTO) {
        Room existRoom = roomMapper.selectById(id);
        if (existRoom == null) {
            throw new BusinessException("包厢不存在");
        }

        // 检查包厢名是否与其他包厢重复
        if (!existRoom.getName().equals(roomDTO.getName())) {
            LambdaQueryWrapper<Room> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Room::getName, roomDTO.getName());
            queryWrapper.ne(Room::getId, id);
            Long count = roomMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException("包厢名称已存在");
            }
        }

        Room room = new Room();
        BeanUtils.copyProperties(roomDTO, room);
        room.setId(id);

        // BugD4修复：若 roomDTO.status 为 null（前端编辑表单不提交 status），
        // BeanUtils 拷贝后 room.status=null，与 existRoom.status 比较 equals(null) 返回 false，
        // 导致 statusChanged=true，每次编辑都冗余触发 Redis 同步且会多查一次 DB。
        // 修复：status 为 null 时保留原值，不视为状态变更。
        if (room.getStatus() == null) {
            room.setStatus(existRoom.getStatus());
        }

        // 如果修改了状态，同步到Redis
        boolean statusChanged = !existRoom.getStatus().equals(room.getStatus());
        boolean updated = roomMapper.updateById(room) > 0;

        if (updated && statusChanged) {
            room = roomMapper.selectById(id);
            syncRoomStatusToRedis(room);
        }

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteRoom(Long id) {
        Room existRoom = roomMapper.selectById(id);
        if (existRoom == null) {
            throw new BusinessException("包厢不存在");
        }

        // 仅允许删除状态为"空闲"的包厢
        if (existRoom.getStatus() != 0) {
            throw new BusinessException("仅允许删除状态为"空闲"的包厢");
        }

        boolean deleted = roomMapper.deleteById(id) > 0;

        // 从Redis中移除
        if (deleted) {
            try {
                stringRedisTemplate.opsForHash().delete(REDIS_ROOM_STATUS_KEY, id.toString());
            } catch (Exception e) {
                log.warn("从Redis中删除包厢状态失败: {}", e.getMessage());
            }
        }

        return deleted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateRoomStatus(Long id, Integer status) {
        Room existRoom = roomMapper.selectById(id);
        if (existRoom == null) {
            throw new BusinessException("包厢不存在");
        }

        // 校验状态值
        if (status < 0 || status > 3) {
            throw new BusinessException("无效的状态值");
        }

        Room room = new Room();
        room.setId(id);
        room.setStatus(status);

        boolean updated = roomMapper.updateById(room) > 0;

        if (updated) {
            room = roomMapper.selectById(id);
            syncRoomStatusToRedis(room);
        }

        return updated;
    }

    @Override
    public RoomVO getRoomById(Long id) {
        Room room = roomMapper.selectById(id);
        if (room == null) {
            throw new BusinessException("包厢不存在");
        }
        return convertToVO(room);
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

            // 使用StringRedisTemplate + JSON序列化，避免Jackson反序列化ClassCastException
            String json = objectMapper.writeValueAsString(roomStatus);
            stringRedisTemplate.opsForHash().put(REDIS_ROOM_STATUS_KEY, room.getId().toString(), json);
            log.debug("包厢状态已同步到Redis: id={}, status={}", room.getId(), room.getStatus());
        } catch (JsonProcessingException e) {
            log.warn("同步包厢状态到Redis失败（序列化错误）: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("同步包厢状态到Redis失败: {}", e.getMessage());
            // Redis操作失败不影响主业务流程
        }
    }

    /**
     * 将Room实体转换为RoomVO
     */
    private RoomVO convertToVO(Room room) {
        RoomVO roomVO = new RoomVO();
        BeanUtils.copyProperties(room, roomVO);
        // 设置状态文本
        roomVO.setStatusText(room.getStatusText());
        return roomVO;
    }
}
