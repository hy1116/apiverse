package com.hypepia.apiverse.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "secret", "test-secret-key-for-unit-testing-only-minimum-256-bits-long!!");
        ReflectionTestUtils.setField(jwtUtils, "expirationDays", 1);
    }

    @Test
    void generateToken_then_parse_returns_same_userId() {
        String token = jwtUtils.generateToken(42L);
        assertThat(jwtUtils.parseUserId(token)).isEqualTo(42L);
    }

    @Test
    void generateToken_different_users_produce_different_tokens() {
        String token1 = jwtUtils.generateToken(1L);
        String token2 = jwtUtils.generateToken(2L);
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void parseUserId_with_invalid_token_throws() {
        assertThatThrownBy(() -> jwtUtils.parseUserId("not-a-jwt"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void parseUserId_with_tampered_signature_throws() {
        String token = jwtUtils.generateToken(1L);
        // 서명 부분(마지막 '.') 이후를 완전히 교체
        String tampered = token.substring(0, token.lastIndexOf('.') + 1)
                + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
        assertThatThrownBy(() -> jwtUtils.parseUserId(tampered))
                .isInstanceOf(Exception.class);
    }
}
