package com.ktv.controller.room;

import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.Result;
import com.ktv.service.HotSongService;
import com.ktv.vo.SongVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 热门歌曲 Controller（包厢端）。
 */
@RestController
@RequestMapping("/api/room/songs")
@RequiredArgsConstructor
public class HotSongController {

    private static final int MAX_LIMIT = 100;

    private final HotSongService hotSongService;

    @GetMapping("/hot")
    public Result<List<SongVO>> getHotSongs(@RequestParam(required = false, defaultValue = "20") Integer limit) {
        if (limit == null || limit <= 0) {
            throw new BusinessException("热门歌曲数量必须大于 0");
        }
        if (limit > MAX_LIMIT) {
            throw new BusinessException("热门歌曲数量不能超过 100");
        }
        List<SongVO> hotSongs = hotSongService.getHotSongs(limit);
        return Result.success(hotSongs);
    }
}
