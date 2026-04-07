package com.ktv.common.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * 消息工具类：根据 MessageKey 获取国际化消息
 *
 * 使用方式：
 * <pre>
 *     // 在 Service 中
 *     throw new BusinessException(MessageHelper.get(MessageKey.SONG_NOT_FOUND));
 * </pre>
 *
 * @author shaun.sheng
 * @since 2026-04-07
 */
@Component
public class MessageHelper {

    private static MessageSource messageSource;

    public MessageHelper(MessageSource messageSource) {
        MessageHelper.messageSource = messageSource;
    }

    /**
     * 获取消息（使用当前线程的 Locale）
     *
     * @param key 消息 Key
     * @return 消息内容
     */
    public static String get(MessageKey key) {
        return get(key, (Object[]) null);
    }

    /**
     * 获取消息（带参数）
     *
     * @param key  消息 Key
     * @param args 参数
     * @return 消息内容
     */
    public static String get(MessageKey key, Object... args) {
        try {
            return messageSource.getMessage(key.getKey(), args, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            // 找不到消息时返回 key 本身
            return key.getKey();
        }
    }
}
