package com.ktv.controller.room;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.Result;
import com.ktv.entity.OrderSong;
import com.ktv.service.PlayQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/room/{orderId}/queue")
@RequiredArgsConstructor
public class PlayQueueController {

    private static final int MAX_PAGE_SIZE = 100;

    private final PlayQueueService playQueueService;

    @PostMapping("/add")
    public Result<Long> addSong(@PathVariable Long orderId, @RequestParam Long songId) {
        validatePositiveId(orderId, "订单 ID 必须为正整数");
        validatePositiveId(songId, "歌曲 ID 必须为正整数");
        log.info("点歌请求: orderId={}, songId={}", orderId, songId);
        Long orderSongId = playQueueService.addSongToQueue(orderId, songId);
        return Result.success("点歌成功", orderSongId);
    }

    @PostMapping("/top/{orderSongId}")
    public Result<Void> topSong(@PathVariable Long orderId, @PathVariable Long orderSongId) {
        validatePositiveId(orderId, "订单 ID 必须为正整数");
        validatePositiveId(orderSongId, "点歌记录 ID 必须为正整数");
        log.info("置顶请求: orderId={}, orderSongId={}", orderId, orderSongId);
        playQueueService.topSong(orderId, orderSongId);
        return Result.success("置顶成功");
    }

    @DeleteMapping("/remove/{orderSongId}")
    public Result<Void> removeSong(@PathVariable Long orderId, @PathVariable Long orderSongId) {
        validatePositiveId(orderId, "订单 ID 必须为正整数");
        validatePositiveId(orderSongId, "点歌记录 ID 必须为正整数");
        log.info("取消点歌请求: orderId={}, orderSongId={}", orderId, orderSongId);
        playQueueService.removeSong(orderId, orderSongId);
        return Result.success("取消点歌成功");
    }

    @GetMapping
    public Result<IPage<OrderSong>> getQueueList(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        validatePositiveId(orderId, "订单 ID 必须为正整数");
        validatePageParams(current, size);
        log.info("查询待唱列表: orderId={}, current={}, size={}", orderId, current, size);
        Page<OrderSong> pageParam = new Page<>(current, size);
        IPage<OrderSong> voPage = playQueueService.getQueueList(pageParam, orderId);
        return Result.success(voPage);
    }

    @GetMapping("/played")
    public Result<IPage<OrderSong>> getPlayedList(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "20") Integer size
    ) {
        validatePositiveId(orderId, "订单 ID 必须为正整数");
        validatePageParams(current, size);
        log.info("查询已唱列表: orderId={}, current={}, size={}", orderId, current, size);
        Page<OrderSong> pageParam = new Page<>(current, size);
        IPage<OrderSong> voPage = playQueueService.getPlayedList(pageParam, orderId);
        return Result.success(voPage);
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
