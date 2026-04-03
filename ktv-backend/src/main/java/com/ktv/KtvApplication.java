package com.ktv;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * KTV点歌系统启动类
 * C9修复：增加JWT密钥安全检查
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@SpringBootApplication
@MapperScan("com.ktv.mapper")
@EnableScheduling
public class KtvApplication {

    public static void main(String[] args) {
        var app = new SpringApplication(KtvApplication.class);
        app.addListeners(event -> {
            if (event instanceof org.springframework.boot.context.event.ApplicationReadyEvent readyEvent) {
                Environment env = readyEvent.getApplicationContext().getEnvironment();
                String jwtSecret = env.getProperty("jwt.secret");
                // C9修复：检查JWT密钥是否为默认值，如果是则发出强烈警告
                if (jwtSecret != null && jwtSecret.contains("local-dev-secret-key")) {
                    System.err.println("""
                            \n
                            ========================================
                            ⚠️  安全警告：JWT密钥使用默认值！
                            ========================================
                            当前JWT_SECRET 使用开发环境默认值。
                            生产环境必须通过环境变量设置强密钥：
                            export JWT_SECRET=<your-strong-secret-key>
                            ========================================
                            """);
                }
            }
        });
        var context = app.run(args);
        System.out.println("""

                ========================================
                  KTV点歌系统启动成功！
                  后端接口地址: http://localhost:8080
                ========================================
                """);
    }

}
