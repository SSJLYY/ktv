package com.ktv.controller.room;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.Result;
import com.ktv.service.SongSearchService;
import com.ktv.vo.CategoryVO;
import com.ktv.vo.SingerVO;
import com.ktv.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 歌曲搜索 Controller（包厢端用）。
 */
@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class SongSearchController {

    private static final long MAX_PAGE_SIZE = 100L;

    private final SongSearchService songSearchService;

    /**
     * 按歌曲名或拼音首字母模糊搜索。
     */
    @GetMapping("/songs/search")
    public Result<IPage<SongVO>> searchSongs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size
    ) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException("搜索关键词不能为空");
        }
        validatePageParams(current, size);
        IPage<SongVO> result = songSearchService.searchSongs(keyword, current, size);
        return Result.success(result);
    }

    /**
     * 按歌手查询歌曲。
     */
    @GetMapping("/songs/by-singer/{singerId}")
    public Result<IPage<SongVO>> getSongsBySinger(
            @PathVariable Long singerId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size
    ) {
        validatePositiveId(singerId, "歌手ID必须为正整数");
        validatePageParams(current, size);
        IPage<SongVO> result = songSearchService.getSongsBySinger(singerId, current, size);
        return Result.success(result);
    }

    /**
     * 按分类查询歌曲。
     */
    @GetMapping("/songs/by-category/{categoryId}")
    public Result<IPage<SongVO>> getSongsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size
    ) {
        validatePositiveId(categoryId, "分类ID必须为正整数");
        validatePageParams(current, size);
        IPage<SongVO> result = songSearchService.getSongsByCategory(categoryId, current, size);
        return Result.success(result);
    }

    /**
     * 获取所有歌手列表。
     */
    @GetMapping("/singers")
    public Result<List<SingerVO>> getAllSingers(
            @RequestParam(required = false) String pinyinInitial
    ) {
        List<SingerVO> result = songSearchService.getAllSingers(pinyinInitial);
        return Result.success(result);
    }

    /**
     * 获取所有分类列表。
     */
    @GetMapping("/categories")
    public Result<List<CategoryVO>> getAllCategories() {
        List<CategoryVO> result = songSearchService.getAllCategories();
        return Result.success(result);
    }

    private void validatePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(message);
        }
    }

    private void validatePageParams(Long current, Long size) {
        if (current == null || current <= 0) {
            throw new BusinessException("页码必须大于0");
        }
        if (size == null || size <= 0) {
            throw new BusinessException("每页数量必须大于0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new BusinessException("每页数量不能超过100");
        }
    }
}
