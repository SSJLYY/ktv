package com.ktv.controller.room;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.entity.OrderSong;
import com.ktv.service.PlayQueueService;
import com.ktv.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 点歌队列Controller
 * 提供点歌队列相关的RESTful API
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Slf4j
@RestController
@RequestMapping("/api/room/{orderId}/queue")
@RequiredArgsConstructor
public class PlayQueueController {

    private final PlayQueueService playQueueService;

    /**
     * 点歌：添加歌曲到排队队列
     *
     * @param orderId 订单ID
     * @param songId  歌曲ID
     * @return 点歌记录ID
     */
    @PostMapping("/add")
    public Result<Long> addSong(@PathVariable Long orderId, @RequestParam Long songId) {
        log.info("点歌请求：订单ID={}, 歌曲ID={}", orderId, songId);

        Long orderSongId = playQueueService.addSongToQueue(orderId, songId);

        return Result.success("点歌成功", orderSongId);
    }

    /**
     * 置顶：将某首歌曲移到队列头部
     *
     * @param orderId     订单ID
     * @param orderSongId 点歌记录ID
     * @return 操作结果
     */
    @PostMapping("/top/{orderSongId}")
    public Result<Void> topSong(@PathVariable Long orderId, @PathVariable Long orderSongId) {
        log.info("置顶请求：订单ID={}, 点歌记录ID={}", orderId, orderSongId);

        playQueueService.topSong(orderId, orderSongId);

        return Result.success("置顶成功");
    }

    /**
     * 取消：从队列中移除歌曲
     *
     * @param orderId     订单ID
     * @param orderSongId 点歌记录ID
     * @return 操作结果
     */
    @DeleteMapping("/remove/{orderSongId}")
    public Result<Void> removeSong(@PathVariable Long orderId, @PathVariable Long orderSongId) {
        log.info("取消点歌请求：订单ID={}, 点歌记录ID={}", orderId, orderSongId);

        playQueueService.removeSong(orderId, orderSongId);

        return Result.success("取消点歌成功");
    }

    /**
     * 查询当前排队列表
     * S14修复：分页参数改为 current/size，与项目规范统一（之前错误地用 page/size）
     *
     * @param orderId 订单ID
     * @param current 页码（默认1）
     * @param size    每页数量（默认20）
     * @return 排队列表
     */
    @GetMapping
    public Result<Page<OrderSong>> getQueueList(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        log.info("查询排队列表：订单ID={}, 页码={}, 每页数量={}", orderId, current, size);

        Page<OrderSong> pageParam = new Page<>(current, size);
        Page<OrderSong> voPage = playQueueService.getQueueList(pageParam, orderId);

        return Result.success(voPage);
    }

    /**
     * 查询已唱列表
     * S14修复：分页参数改为 current/size
     *
     * @param orderId 订单ID
     * @param current 页码（默认1）
     * @param size    每页数量（默认20）
     * @return 已唱列表
     */
    @GetMapping("/played")
    public Result<Page<OrderSong>> getPlayedList(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        log.info("查询已唱列表：订单ID={}, 页码={}, 每页数量={}", orderId, current, size);

        Page<OrderSong> pageParam = new Page<>(current, size);
        Page<OrderSong> voPage = playQueueService.getPlayedList(pageParam, orderId);

        return Result.success(voPage);
    }
}
