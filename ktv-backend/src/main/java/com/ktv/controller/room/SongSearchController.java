package com.ktv.controller.room;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ktv.common.result.Result;
import com.ktv.service.SongSearchService;
import com.ktv.vo.CategoryVO;
import com.ktv.vo.SingerVO;
import com.ktv.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 歌曲检索Controller（包厢端用）
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class SongSearchController {

    private final SongSearchService songSearchService;

    /**
     * 按歌曲名或拼音首字母模糊搜索
     * S15修复：分页参数改为 current/size，与项目规范统一（之前错误地用 pageNum/pageSize）
     * GET /api/room/songs/search?keyword=xxx
     */
    @GetMapping("/songs/search")
    public Result<IPage<SongVO>> searchSongs(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size
    ) {
        IPage<SongVO> result = songSearchService.searchSongs(keyword, current, size);
        return Result.success(result);
    }

    /**
     * 按歌手查询歌曲（分页）
     * S15修复：分页参数改为 current/size
     * GET /api/room/songs/by-singer/{singerId}
     */
    @GetMapping("/songs/by-singer/{singerId}")
    public Result<IPage<SongVO>> getSongsBySinger(
            @PathVariable Long singerId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size
    ) {
        IPage<SongVO> result = songSearchService.getSongsBySinger(singerId, current, size);
        return Result.success(result);
    }

    /**
     * 按分类查询歌曲（分页）
     * S15修复：分页参数改为 current/size
     * GET /api/room/songs/by-category/{categoryId}
     */
    @GetMapping("/songs/by-category/{categoryId}")
    public Result<IPage<SongVO>> getSongsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "20") Long size
    ) {
        IPage<SongVO> result = songSearchService.getSongsByCategory(categoryId, current, size);
        return Result.success(result);
    }

    /**
     * 获取所有歌手列表
     * GET /api/room/singers
     */
    @GetMapping("/singers")
    public Result<List<SingerVO>> getAllSingers(
            @RequestParam(required = false) String pinyinInitial
    ) {
        List<SingerVO> result = songSearchService.getAllSingers(pinyinInitial);
        return Result.success(result);
    }

    /**
     * 获取所有分类列表
     * GET /api/room/categories
     */
    @GetMapping("/categories")
    public Result<List<CategoryVO>> getAllCategories() {
        List<CategoryVO> result = songSearchService.getAllCategories();
        return Result.success(result);
    }
}
