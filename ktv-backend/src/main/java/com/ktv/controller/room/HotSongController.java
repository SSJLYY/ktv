package com.ktv.controller.room;

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
 * 热门歌曲Controller（包厢端）
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@RestController
@RequestMapping("/api/room/songs")
@RequiredArgsConstructor
public class HotSongController {

    private final HotSongService hotSongService;

    /**
     * 获取热门歌曲排行榜
     *
     * @param limit 前N首，默认20
     * @return 热门歌曲列表
     */
    @GetMapping("/hot")
    public Result<List<SongVO>> getHotSongs(@RequestParam(required = false, defaultValue = "20") Integer limit) {
        List<SongVO> hotSongs = hotSongService.getHotSongs(limit);
        return Result.success(hotSongs);
    }
}
