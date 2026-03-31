package com.ktv.controller.room;

import com.ktv.dto.CurrentPlayVO;
import com.ktv.service.PlayControlService;
import com.ktv.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 播放控制Controller
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Tag(name = "播放控制", description = "播放控制相关接口")
@RestController
@RequestMapping("/api/room/{orderId}/play")
@RequiredArgsConstructor
public class PlayControlController {

    private final PlayControlService playControlService;

    /**
     * 切歌（下一首）
     */
    @Operation(summary = "切歌（下一首）", description = "将当前歌曲标记为已播放，取队列下一首设为播放中")
    @PostMapping("/next")
    public Result<Void> next(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        playControlService.next(orderId);
        return Result.success();
    }

    /**
     * 重唱
     */
    @Operation(summary = "重唱", description = "重置当前歌曲状态为播放中（重唱不换歌）")
    @PostMapping("/replay")
    public Result<Void> replay(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        playControlService.replay(orderId);
        return Result.success();
    }

    /**
     * 暂停播放
     */
    @Operation(summary = "暂停播放", description = "更新Redis中播放状态为已暂停")
    @PostMapping("/pause")
    public Result<Void> pause(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        playControlService.pause(orderId);
        return Result.success();
    }

    /**
     * 恢复播放
     */
    @Operation(summary = "恢复播放", description = "更新Redis中播放状态为播放中")
    @PostMapping("/resume")
    public Result<Void> resume(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        playControlService.resume(orderId);
        return Result.success();
    }

    /**
     * 查询当前播放状态
     */
    @Operation(summary = "查询当前播放状态", description = "返回当前播放歌曲信息、队列剩余数量")
    @GetMapping("/current")
    public Result<CurrentPlayVO> getCurrentPlayStatus(
            @Parameter(description = "订单ID") @PathVariable Long orderId) {
        CurrentPlayVO vo = playControlService.getCurrentPlayStatus(orderId);
        return Result.success(vo);
    }
}
