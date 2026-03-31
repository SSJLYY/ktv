package com.ktv.controller;

import com.ktv.common.exception.BusinessException;
import com.ktv.common.result.Result;
import com.ktv.common.util.PinyinUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 * 用于测试公共基础类功能
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Slf4j
@RestController
@RequestMapping("/test")
@Profile("dev")
public class TestController {

    /**
     * 测试统一返回结果
     */
    @GetMapping("/result")
    public Result<?> testResult() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "测试统一返回结果");
        data.put("timestamp", System.currentTimeMillis());
        return Result.success(data);
    }

    /**
     * 测试异常处理
     */
    @GetMapping("/exception")
    public Result<?> testException() {
        throw new BusinessException("这是一个业务异常测试");
    }

    /**
     * 测试参数校验异常
     */
    @GetMapping("/param-error")
    public Result<?> testParamError(@RequestParam(required = false) String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("参数name不能为空");
        }
        return Result.success("参数校验通过：" + name);
    }

    /**
     * 测试拼音工具
     */
    @GetMapping("/pinyin")
    public Result<?> testPinyin(@RequestParam String text) {
        Map<String, Object> result = new HashMap<>();
        result.put("原文", text);
        result.put("拼音全拼", PinyinUtil.getPinyin(text));
        result.put("拼音首字母", PinyinUtil.getPinyinInitial(text));
        result.put("首字母", PinyinUtil.getFirstLetter(text));
        return Result.success(result);
    }

    /**
     * 测试成功响应
     */
    @GetMapping("/success")
    public Result<?> testSuccess() {
        return Result.success("操作成功");
    }

    /**
     * 测试失败响应
     */
    @GetMapping("/fail")
    public Result<?> testFail() {
        return Result.fail("操作失败");
    }

}
