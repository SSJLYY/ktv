package com.ktv.controller.admin;

import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.Result;
import com.ktv.dto.RoomDTO;
import com.ktv.service.RoomService;
import com.ktv.util.AdminAccessUtils;
import com.ktv.vo.RoomVO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 包厢管理 Controller。
 */
@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
@Validated
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public Result<List<RoomVO>> getRoomList(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String type) {
        validateOptionalStatus(status);
        List<RoomVO> list = roomService.getRoomList(status, type);
        return Result.success(list);
    }

    @GetMapping("/available")
    public Result<List<RoomVO>> getAvailableRooms() {
        List<RoomVO> list = roomService.getAvailableRooms();
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<RoomVO> getRoomById(@PathVariable Long id) {
        validatePositiveId(id, "包厢 ID 必须为正整数");
        RoomVO roomVO = roomService.getRoomById(id);
        return Result.success(roomVO);
    }

    @PostMapping
    public Result<Long> createRoom(
            @Validated(RoomDTO.Create.class) @RequestBody RoomDTO roomDTO,
            @RequestAttribute(name = "userId", required = false) Long userId,
            @RequestAttribute(name = "role", required = false) String role) {
        AdminAccessUtils.requireSuperAdmin(userId, role);
        Long id = roomService.createRoom(roomDTO);
        return Result.success(id);
    }

    @PutMapping("/{id}")
    public Result<Boolean> updateRoom(
            @PathVariable Long id,
            @Validated(RoomDTO.Update.class) @RequestBody RoomDTO roomDTO,
            @RequestAttribute(name = "userId", required = false) Long userId,
            @RequestAttribute(name = "role", required = false) String role) {
        validatePositiveId(id, "包厢 ID 必须为正整数");
        AdminAccessUtils.requireSuperAdmin(userId, role);
        Boolean success = roomService.updateRoom(id, roomDTO);
        return Result.success(success);
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> deleteRoom(
            @PathVariable Long id,
            @RequestAttribute(name = "userId", required = false) Long userId,
            @RequestAttribute(name = "role", required = false) String role) {
        validatePositiveId(id, "包厢 ID 必须为正整数");
        AdminAccessUtils.requireSuperAdmin(userId, role);
        Boolean success = roomService.deleteRoom(id);
        return Result.success(success);
    }

    @PutMapping("/{id}/status")
    public Result<Boolean> updateRoomStatus(
            @PathVariable Long id,
            @RequestParam Integer status,
            @RequestAttribute(name = "userId", required = false) Long userId,
            @RequestAttribute(name = "role", required = false) String role) {
        validatePositiveId(id, "包厢 ID 必须为正整数");
        validateOptionalStatus(status);
        AdminAccessUtils.requireSuperAdmin(userId, role);
        Boolean success = roomService.updateRoomStatus(id, status);
        return Result.success(success);
    }

    private void validatePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(message);
        }
    }

    private void validateOptionalStatus(Integer status) {
        if (status != null && (status < 0 || status > 3)) {
            throw new BusinessException("包厢状态值只能是 0、1、2 或 3");
        }
    }
}
