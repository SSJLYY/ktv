package com.ktv.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

/**
 * 文件安全检测工具类
 *
 * 提供多层文件安全防护：
 * 1. 文件魔数（Magic Number）验证：检查文件头是否与声称的扩展名匹配，防止伪造扩展名上传恶意文件
 * 2. 可扩展的病毒扫描接口：预留 ClamAV 等外部杀毒引擎集成接口
 *
 * @author shaun.sheng
 * @since 2026-04-07
 */
@Slf4j
@Component
public class FileSecurityChecker {

    /**
     * 是否启用严格的魔数检测
     * 开发环境可设为 false（允许非标准文件头），生产环境应设为 true
     */
    @Value("${ktv.security.magic-check-enabled:true}")
    private boolean magicCheckEnabled;

    /**
     * 魔数检测时读取的最大字节数
     */
    private static final int MAGIC_BYTES_READ = 12;

    /**
     * 文件扩展名 → 魔数前缀映射表
     * 值为 Hex 字符串形式，如 "FFD8FF" 表示 JPEG 的文件头
     */
    private static final Map<String, Set<String>> EXTENSION_MAGIC_MAP = Map.of(
            // 音频文件
            "mp3", Set.of("ID3", "FFFB", "FFF3", "FFF2"),  // ID3 tag 或 MPEG audio frame sync
            "flac", Set.of("664C6143"),                      // "fLaC"
            "wav", Set.of("52494646"),                        // RIFF
            "ogg", Set.of("4F676753"),                        // OggS
            "m4a", Set.of("0000001866747970"),               // ftyp (MPEG-4)
            // 视频文件
            "mp4", Set.of("0000001866747970", "0000002066747970"), // ftyp
            "avi", Set.of("52494646"),                              // RIFF (与WAV共享，需结合扩展名判断)
            "mkv", Set.of("1A45DFA3"),                              // EBML header
            "webm", Set.of("1A45DFA3"),                             // EBML header (与MKV共享)
            // 图片文件
            "jpg", Set.of("FFD8FF"),
            "jpeg", Set.of("FFD8FF"),
            "png", Set.of("89504E47"),                              // PNG signature
            "gif", Set.of("47494638"),                              // "GIF8"
            "webp", Set.of("52494646")                               // RIFF (WebP)
    );

    /**
     * 不支持魔数检测的扩展名集合（格式自由或无固定文件头）
     * 这些格式仍会通过扩展名+MIME白名单进行安全检查
     */
    private static final Set<String> SKIP_MAGIC_CHECK_EXTENSIONS = Set.of(
            "avi"  // AVI 使用 RIFF 容器，与 WAV 共享文件头，仅靠扩展名+MIME区分
    );

    /**
     * 对上传的文件进行全面安全检测
     *
     * @param filePath      保存后的文件路径
     * @param originalName  原始文件名（用于提取扩展名）
     * @param contentType   HTTP 请求中的 Content-Type
     * @return 安全检测结果
     */
    public SecurityCheckResult check(Path filePath, String originalName, String contentType) {
        String extension = getExtension(originalName);

        // 1. 扩展名白名单检查
        if (!isAllowedExtension(extension)) {
            log.warn("文件安全检查失败：不支持的文件扩展名 ext={}, file={}", extension, originalName);
            return SecurityCheckResult.fail("不支持的文件类型：" + extension);
        }

        // 2. 魔数（Magic Number）验证
        if (magicCheckEnabled && !SKIP_MAGIC_CHECK_EXTENSIONS.contains(extension)) {
            try {
                MagicCheckResult magicResult = verifyMagicNumber(filePath, extension);
                if (!magicResult.passed()) {
                    log.warn("文件安全检查失败：魔数不匹配 ext={}, file={}, reason={}",
                            extension, originalName, magicResult.reason());
                    return SecurityCheckResult.fail("文件内容与类型不匹配，可能存在伪造");
                }
            } catch (IOException e) {
                log.warn("文件魔数检测IO异常，跳过检测：{}", e.getMessage());
                // IO异常不阻塞上传，仅记录警告
            }
        }

        // 3. 病毒扫描（预留接口，可集成 ClamAV 等）
        // TODO: 集成 ClamAV 时取消注释
        // VirusScanResult virusResult = scanForViruses(filePath);
        // if (!virusResult.isClean()) {
        //     log.warn("文件安全检查失败：病毒检测 ext={}, file={}, virus={}",
        //             extension, originalName, virusResult.getVirusName());
        //     return SecurityCheckResult.fail("文件未通过病毒扫描：" + virusResult.getVirusName());
        // }

        log.debug("文件安全检查通过：ext={}, file={}, size={}",
                extension, originalName, Files.exists(filePath) ? filePath.toFile().length() : -1);
        return SecurityCheckResult.pass();
    }

    /**
     * 验证文件魔数是否与扩展名匹配
     *
     * @param filePath   文件路径
     * @param extension  声称的文件扩展名
     * @return 魔数检测结果
     */
    private MagicCheckResult verifyMagicNumber(Path filePath, String extension) throws IOException {
        Set<String> expectedMagicSet = EXTENSION_MAGIC_MAP.get(extension);
        if (expectedMagicSet == null || expectedMagicSet.isEmpty()) {
            return new MagicCheckResult(true, "无魔数规则，跳过检测");
        }

        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] header = new byte[MAGIC_BYTES_READ];
            int bytesRead = is.read(header);
            if (bytesRead <= 0) {
                return new MagicCheckResult(false, "文件为空或无法读取");
            }

            String headerHex = HexFormat.of().formatHex(header, 0, Math.min(bytesRead, MAGIC_BYTES_READ));
            String headerAscii = new String(header, 0, Math.min(bytesRead, MAGIC_BYTES_READ));

            for (String expectedMagic : expectedMagicSet) {
                // 尝试 Hex 匹配
                if (headerHex.startsWith(expectedMagic.toLowerCase())) {
                    return new MagicCheckResult(true, null);
                }
                // 尝试 ASCII 匹配（如 "ID3", "fLaC"）
                if (headerAscii.startsWith(expectedMagic)) {
                    return new MagicCheckResult(true, null);
                }
            }

            return new MagicCheckResult(false,
                    "文件头不匹配：期望[" + String.join("|", expectedMagicSet) + "]，实际[" + headerHex + "]");
        }
    }

    /**
     * 获取文件扩展名（小写）
     */
    private String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }

    /**
     * 检查扩展名是否在允许的范围内
     */
    private boolean isAllowedExtension(String extension) {
        return EXTENSION_MAGIC_MAP.containsKey(extension);
    }

    // ========== 内部记录类型 ==========

    /**
     * 安全检测结果
     */
    public record SecurityCheckResult(boolean passed, String message) {
        static SecurityCheckResult pass() {
            return new SecurityCheckResult(true, "通过");
        }
        static SecurityCheckResult fail(String reason) {
            return new SecurityCheckResult(false, reason);
        }
    }

    /**
     * 魔数检测结果
     */
    private record MagicCheckResult(boolean passed, String reason) {
    }
}
