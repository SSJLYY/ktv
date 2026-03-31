package com.ktv.util;

import com.ktv.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 订单号生成工具
 * 格式：KTV + yyyyMMdd + 6位序号
 * 例如：KTV20260330000001
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@Component
@RequiredArgsConstructor
public class OrderNoUtil {

    private final OrderMapper orderMapper;

    /**
     * 日期格式化
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 生成下一个订单编号
     *
     * @return 订单编号
     */
    public synchronized String generateOrderNo() {
        String dateStr = LocalDate.now().format(DATE_FORMATTER);
        String prefix = "KTV" + dateStr;

        // 查询当天最大序号
        Integer maxSeq = orderMapper.selectMaxSeqByDate(dateStr);
        int nextSeq = (maxSeq == null ? 0 : maxSeq) + 1;

        // 格式化为6位序号
        return prefix + String.format("%06d", nextSeq);
    }
}
