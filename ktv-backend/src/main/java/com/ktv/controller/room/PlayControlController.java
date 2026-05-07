package com.ktv.controller.room;

import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.Result;
import com.ktv.dto.CurrentPlayVO;
import com.ktv.service.PlayControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "播放控制", description = "播放控制相关接口")
@RestController
@RequestMapping("/api/room/{orderId}/play")
@RequiredArgsConstructor
public class PlayControlController {

    private final PlayControlService playControlService;

    @Operation(summary = "切歌", description = "将当前歌曲标记为已播放，并切换到队列中的下一首")
    @PostMapping("/next")
    public Result<Void> next(@Parameter(description = "订单 ID") @PathVariable Long orderId) {
        validatePositiveId(orderId, "订单 ID 必须为正整数");
        playControlService.next(orderId);
        return Result.success();
    }

    @Operation(summary = "重唱", description = "重新播放当前歌曲，不切换队列")
    @PostMapping("/replay")
    public Result<Void> replay(@Parameter(description = "订单 ID") @PathVariable Long orderId) {
        validatePositiveId(orderId, "订单 ID 必须为正整数");
        playControlService.replay(orderId);
        return Result.success();
    }

    @Operation(summary = "暂停播放", description = "将当前播放状态更新为暂停")
    @PostMapping("/pause")
    public Result<Void> pause(@Parameter(description = "订单 ID") @PathVariable Long orderId) {
        validatePositiveId(orderId, "订单 ID 必须为正整数");
        playControlService.pause(orderId);
        return Result.success();
    }

    @Operation(summary = "继续播放", description = "将当前播放状态更新为播放中")
    @PostMapping("/resume")
    public Result<Void> resume(@Parameter(description = "订单 ID") @PathVariable Long orderId) {
        validatePositiveId(orderId, "订单 ID 必须为正整数");
        playControlService.resume(orderId);
        return Result.success();
    }

    @Operation(summary = "查询当前播放状态", description = "返回当前播放歌曲信息和剩余待唱数量")
    @GetMapping("/current")
    public Result<CurrentPlayVO> getCurrentPlayStatus(@Parameter(description = "订单 ID") @PathVariable Long orderId) {
        validatePositiveId(orderId, "订单 ID 必须为正整数");
        CurrentPlayVO vo = playControlService.getCurrentPlayStatus(orderId);
        return Result.success(vo);
    }

    private void validatePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(message);
        }
    }
}
