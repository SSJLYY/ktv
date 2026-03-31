package com.ktv;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * KTV点歌系统启动类
 *
 * @author shaun.sheng
 * @since 2026-03-30
 */
@SpringBootApplication
@MapperScan("com.ktv.mapper")
@EnableScheduling
public class KtvApplication {

    public static void main(String[] args) {
        SpringApplication.run(KtvApplication.class, args);
        System.out.println("""

                ========================================
                  KTV点歌系统启动成功！
                  后端接口地址: http://localhost:8080
                ========================================
                """);
    }

}
