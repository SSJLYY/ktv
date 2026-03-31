package com.ktv.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ktv.dto.RoomDTO;
import com.ktv.entity.Room;
import com.ktv.vo.RoomVO;

import java.util.List;

/**
 * 包厢Service
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
public interface RoomService extends IService<Room> {

    /**
     * 获取包厢列表（支持按状态和类型筛选）
     *
     * @param status 状态（可选）
     * @param type 类型（可选）
     * @return 包厢VO列表
     */
    List<RoomVO> getRoomList(Integer status, String type);

    /**
     * 获取空闲包厢列表
     *
     * @return 空闲包厢列表
     */
    List<RoomVO> getAvailableRooms();

    /**
     * 新增包厢
     *
     * @param roomDTO 包厢DTO
     * @return 包厢ID
     */
    Long createRoom(RoomDTO roomDTO);

    /**
     * 修改包厢信息
     *
     * @param id 包厢ID
     * @param roomDTO 包厢DTO
     * @return 是否成功
     */
    Boolean updateRoom(Long id, RoomDTO roomDTO);

    /**
     * 删除包厢（仅允许删除状态为"空闲"的包厢）
     *
     * @param id 包厢ID
     * @return 是否成功
     */
    Boolean deleteRoom(Long id);

    /**
     * 更新包厢状态（同步更新Redis状态快照）
     *
     * @param id 包厢ID
     * @param status 新状态
     * @return 是否成功
     */
    Boolean updateRoomStatus(Long id, Integer status);

    /**
     * 根据ID获取包厢详情
     *
     * @param id 包厢ID
     * @return 包厢VO
     */
    RoomVO getRoomById(Long id);

    /**
     * 同步包厢状态到Redis
     *
     * @param room 包厢实体
     */
    void syncRoomStatusToRedis(Room room);
}
