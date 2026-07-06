package com.hypepia.apiverse.gateway.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Apiverse Gateway API",
                description = "회원가입/로그인, API 상품 마켓플레이스, API 키 발급, 사용량 조회, 문의. "
                        + "프록시 호출(/gateway/{code}/**)은 X-API-KEY 헤더로 인증하며 이 문서에는 포함되지 않는다.",
                version = "v1"
        ),
        servers = @Server(url = "/gateway")
)
@SecurityScheme(
        name = "bearerAuth",
        type = io.swagger.v3.oas.annotations.enums.SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
