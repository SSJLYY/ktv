package com.ktv.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.common.result.Result;
import com.ktv.dto.SingerDTO;
import com.ktv.service.SingerService;
import com.ktv.vo.SingerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 歌手管理Controller
 * 
 * @author shaun.sheng
 * @since 2026-03-30
 */
@RestController
@RequestMapping("/api/admin/singers")
@RequiredArgsConstructor
@Validated
public class SingerController {

    private final SingerService singerService;

    /**
     * 分页查询歌手列表（带筛选）
     * 
     * @param current 当前页
     * @param size 每页大小
     * @param name 歌手名（可选）
     * @param region 地区（可选）
     * @return 分页结果
     */
    @GetMapping
    public Result<IPage<SingerVO>> getSingerPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String region) {
        
        IPage<SingerVO> page = singerService.getSingerPage(current, size, name, region);
        return Result.success(page);
    }

    /**
     * 新增歌手
     * 
     * @param singerDTO 歌手DTO
     * @return 歌手ID
     */
    @PostMapping
    public Result<Long> createSinger(@Valid @RequestBody SingerDTO singerDTO) {
        Long id = singerService.createSinger(singerDTO);
        return Result.success(id);
    }

    /**
     * 根据ID获取歌手详情
     * 
     * @param id 歌手ID
     * @return 歌手VO
     */
    @GetMapping("/{id}")
    public Result<SingerVO> getSingerById(@PathVariable Long id) {
        SingerVO singerVO = singerService.getSingerById(id);
        return Result.success(singerVO);
    }

    /**
     * 修改歌手
     * 
     * @param id 歌手ID
     * @param singerDTO 歌手DTO
     * @return 是否成功
     */
    @PutMapping("/{id}")
    public Result<Boolean> updateSinger(@PathVariable Long id,
                                        @Valid @RequestBody SingerDTO singerDTO) {
        Boolean success = singerService.updateSinger(id, singerDTO);
        return Result.success(success);
    }

    /**
     * 删除歌手（逻辑删除）
     * 
     * @param id 歌手ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteSinger(@PathVariable Long id) {
        Boolean success = singerService.deleteSinger(id);
        return Result.success(success);
    }
}
