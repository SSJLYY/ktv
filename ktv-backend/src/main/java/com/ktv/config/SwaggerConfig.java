package com.ktv.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 配置
 * 访问地址：http://localhost:8080/swagger-ui.html
 *
 * @author shaun.sheng
 * @since 2026-04-07
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("KTV 点歌系统 API 文档")
                        .description("KTV 点歌管理系统后端 API 接口文档\n\n"
                                + "## 模块说明\n"
                                + "- **管理端 API** (`/api/admin/`)：需要 JWT Token 认证\n"
                                + "- **包厢端 API** (`/api/room/`)：无需认证，供包厢点歌终端使用\n")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("shaun.sheng"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Token"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Token",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("输入 JWT Token（登录接口返回的 token 字段）")));
    }
}
