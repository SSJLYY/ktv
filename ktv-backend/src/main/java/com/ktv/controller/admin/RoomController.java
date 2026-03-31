package com.ktv.controller.admin;

import com.ktv.common.result.Result;
import com.ktv.dto.RoomDTO;
import com.ktv.service.RoomService;
import com.ktv.vo.RoomVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 包厢管理Controller
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
@Validated
public class RoomController {

    private final RoomService roomService;

    /**
     * 获取包厢列表（支持按状态和类型筛选）
     *
     * @param status 状态（可选）：0空闲 1使用中 2清洁中 3维修中
     * @param type 类型（可选）：小包/中包/大包/豪华包
     * @return 包厢列表
     */
    @GetMapping
    public Result<List<RoomVO>> getRoomList(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String type) {
        List<RoomVO> list = roomService.getRoomList(status, type);
        return Result.success(list);
    }

    /**
     * 获取空闲包厢列表（用于开台选择）
     *
     * @return 空闲包厢列表
     */
    @GetMapping("/available")
    public Result<List<RoomVO>> getAvailableRooms() {
        List<RoomVO> list = roomService.getAvailableRooms();
        return Result.success(list);
    }

    /**
     * 根据ID获取包厢详情
     *
     * @param id 包厢ID
     * @return 包厢详情
     */
    @GetMapping("/{id}")
    public Result<RoomVO> getRoomById(@PathVariable Long id) {
        RoomVO roomVO = roomService.getRoomById(id);
        return Result.success(roomVO);
    }

    /**
     * 新增包厢
     *
     * @param roomDTO 包厢DTO
     * @return 包厢ID
     */
    @PostMapping
    public Result<Long> createRoom(@Valid @RequestBody RoomDTO roomDTO) {
        Long id = roomService.createRoom(roomDTO);
        return Result.success(id);
    }

    /**
     * 修改包厢信息
     *
     * @param id 包厢ID
     * @param roomDTO 包厢DTO
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public Result<Boolean> updateRoom(@PathVariable Long id,
                                      @Valid @RequestBody RoomDTO roomDTO) {
        Boolean success = roomService.updateRoom(id, roomDTO);
        return Result.success(success);
    }

    /**
     * 删除包厢（仅允许删除状态为"空闲"的包厢）
     *
     * @param id 包厢ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteRoom(@PathVariable Long id) {
        Boolean success = roomService.deleteRoom(id);
        return Result.success(success);
    }

    /**
     * 更新包厢状态（同步更新Redis中的状态快照）
     *
     * @param id 包厢ID
     * @param status 新状态：0空闲 1使用中 2清洁中 3维修中
     * @return 是否成功
     */
    @PutMapping("/{id}/status")
    public Result<Boolean> updateRoomStatus(@PathVariable Long id,
                                            @RequestParam Integer status) {
        Boolean success = roomService.updateRoomStatus(id, status);
        return Result.success(success);
    }
}
