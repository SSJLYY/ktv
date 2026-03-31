package com.ktv.service;

import com.ktv.vo.SongVO;

import java.util.List;

/**
 * 热门歌曲Service接口
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
public interface HotSongService {

    /**
     * 获取热门歌曲排行榜
     *
     * @param limit 前N首
     * @return 热门歌曲列表
     */
    List<SongVO> getHotSongs(Integer limit);

    /**
     * 增加歌曲热度（点歌时调用）
     *
     * @param songId 歌曲ID
     */
    void incrementHotScore(Long songId);

    /**
     * 预热热门榜（从数据库读取并初始化Redis）
     */
    void warmUpHotSongs();

    /**
     * 同步热门分数到数据库
     */
    void syncHotScoreToDb();
}
