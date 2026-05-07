package com.ktv.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.Result;
import com.ktv.dto.SingerDTO;
import com.ktv.service.SingerService;
import com.ktv.util.AdminAccessUtils;
import com.ktv.vo.SingerVO;
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

/**
 * 歌手管理 Controller。
 */
@RestController
@RequestMapping("/api/admin/singers")
@RequiredArgsConstructor
@Validated
public class SingerController {

    private static final int MAX_PAGE_SIZE = 100;

    private final SingerService singerService;

    /**
     * 分页查询歌手列表。
     */
    @GetMapping
    public Result<IPage<SingerVO>> getSingerPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String region) {
        validatePageParams(current, size);
        IPage<SingerVO> page = singerService.getSingerPage(current, size, name, region);
        return Result.success(page);
    }

    /**
     * 新增歌手。
     */
    @PostMapping
    public Result<Long> createSinger(
            @Validated(SingerDTO.Create.class) @RequestBody SingerDTO singerDTO,
            @RequestAttribute(name = "userId", required = false) Long userId,
            @RequestAttribute(name = "role", required = false) String role) {
        AdminAccessUtils.requireSuperAdmin(userId, role);
        Long id = singerService.createSinger(singerDTO);
        return Result.success(id);
    }

    /**
     * 根据 ID 获取歌手详情。
     */
    @GetMapping("/{id}")
    public Result<SingerVO> getSingerById(@PathVariable Long id) {
        validatePositiveId(id, "歌手 ID 必须为正整数");
        SingerVO singerVO = singerService.getSingerById(id);
        return Result.success(singerVO);
    }

    /**
     * 修改歌手。
     */
    @PutMapping("/{id}")
    public Result<Boolean> updateSinger(
            @PathVariable Long id,
            @Validated(SingerDTO.Update.class) @RequestBody SingerDTO singerDTO,
            @RequestAttribute(name = "userId", required = false) Long userId,
            @RequestAttribute(name = "role", required = false) String role) {
        validatePositiveId(id, "歌手 ID 必须为正整数");
        AdminAccessUtils.requireSuperAdmin(userId, role);
        Boolean success = singerService.updateSinger(id, singerDTO);
        return Result.success(success);
    }

    /**
     * 删除歌手。
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteSinger(
            @PathVariable Long id,
            @RequestAttribute(name = "userId", required = false) Long userId,
            @RequestAttribute(name = "role", required = false) String role) {
        validatePositiveId(id, "歌手 ID 必须为正整数");
        AdminAccessUtils.requireSuperAdmin(userId, role);
        Boolean success = singerService.deleteSinger(id);
        return Result.success(success);
    }

    private void validatePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(message);
        }
    }

    private void validatePageParams(Integer current, Integer size) {
        if (current == null || current <= 0) {
            throw new BusinessException("页码必须大于 0");
        }
        if (size == null || size <= 0) {
            throw new BusinessException("每页数量必须大于 0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new BusinessException("每页数量不能超过 100");
        }
    }
}
