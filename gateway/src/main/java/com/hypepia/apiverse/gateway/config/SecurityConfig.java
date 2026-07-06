package com.hypepia.apiverse.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, JwtUtils jwtUtils) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .addFilterBefore(new JwtWebFilter(jwtUtils), SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/products/pending", "/api/products/my").authenticated()
                        .pathMatchers(HttpMethod.GET, "/api/products", "/api/products/*", "/api/products/by-code/*").permitAll()
                        .pathMatchers("/api/usage/**").authenticated()
                        .pathMatchers("/gateway/**").permitAll()  // API key auth handled in ProxyService
                        // springdoc — Swagger UI / OpenAPI 문서
                        .pathMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/webjars/**").permitAll()
                        .anyExchange().authenticated()
                )
                .build();
    }
}
