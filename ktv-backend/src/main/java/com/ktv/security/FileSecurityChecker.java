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

@Slf4j
@Component
public class FileSecurityChecker {

    @Value("${ktv.security.magic-check-enabled:true}")
    private boolean magicCheckEnabled;

    private static final int MAGIC_BYTES_READ = 12;

    private static final Map<String, Set<String>> EXTENSION_MAGIC_MAP = Map.ofEntries(
            Map.entry("mp3", Set.of("ID3", "FFFB", "FFF3", "FFF2")),
            Map.entry("flac", Set.of("664C6143")),
            Map.entry("wav", Set.of("52494646")),
            Map.entry("ogg", Set.of("4F676753")),
            Map.entry("m4a", Set.of("0000001866747970")),
            Map.entry("mp4", Set.of("0000001866747970", "0000002066747970")),
            Map.entry("avi", Set.of("52494646")),
            Map.entry("mkv", Set.of("1A45DFA3")),
            Map.entry("webm", Set.of("1A45DFA3")),
            Map.entry("jpg", Set.of("FFD8FF")),
            Map.entry("jpeg", Set.of("FFD8FF")),
            Map.entry("png", Set.of("89504E47")),
            Map.entry("gif", Set.of("47494638")),
            Map.entry("webp", Set.of("52494646"))
    );

    private static final Set<String> SKIP_MAGIC_CHECK_EXTENSIONS = Set.of("avi");

    public SecurityCheckResult check(Path filePath, String originalName, String contentType) {
        String extension = getExtension(originalName);

        if (!isAllowedExtension(extension)) {
            log.warn("File security check failed: unsupported extension, ext={}, file={}", extension, originalName);
            return SecurityCheckResult.fail("Unsupported file type: " + extension);
        }

        if (magicCheckEnabled && !SKIP_MAGIC_CHECK_EXTENSIONS.contains(extension)) {
            try {
                MagicCheckResult magicResult = verifyMagicNumber(filePath, extension);
                if (!magicResult.passed()) {
                    log.warn("File security check failed: magic mismatch, ext={}, file={}, reason={}",
                            extension, originalName, magicResult.reason());
                    return SecurityCheckResult.fail("File content does not match extension");
                }
            } catch (IOException e) {
                log.warn("Magic number check skipped because of IO error: {}", e.getMessage());
            }
        }

        return SecurityCheckResult.pass();
    }

    private MagicCheckResult verifyMagicNumber(Path filePath, String extension) throws IOException {
        Set<String> expectedMagicSet = EXTENSION_MAGIC_MAP.get(extension);
        if (expectedMagicSet == null || expectedMagicSet.isEmpty()) {
            return new MagicCheckResult(true, "No magic rule");
        }

        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] header = new byte[MAGIC_BYTES_READ];
            int bytesRead = is.read(header);
            if (bytesRead <= 0) {
                return new MagicCheckResult(false, "Empty file");
            }

            String headerHex = HexFormat.of().formatHex(header, 0, Math.min(bytesRead, MAGIC_BYTES_READ)).toUpperCase();
            String headerAscii = new String(header, 0, Math.min(bytesRead, MAGIC_BYTES_READ));

            for (String expectedMagic : expectedMagicSet) {
                if (headerHex.startsWith(expectedMagic)) {
                    return new MagicCheckResult(true, null);
                }
                if (headerAscii.startsWith(expectedMagic)) {
                    return new MagicCheckResult(true, null);
                }
            }

            return new MagicCheckResult(false,
                    "Expected one of [" + String.join("|", expectedMagicSet) + "], actual [" + headerHex + "]");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1).toLowerCase() : "";
    }

    private boolean isAllowedExtension(String extension) {
        return EXTENSION_MAGIC_MAP.containsKey(extension);
    }

    public record SecurityCheckResult(boolean passed, String message) {
        static SecurityCheckResult pass() {
            return new SecurityCheckResult(true, "OK");
        }

        static SecurityCheckResult fail(String reason) {
            return new SecurityCheckResult(false, reason);
        }
    }

    private record MagicCheckResult(boolean passed, String reason) {
    }
}
