package com.hypepia.apiverse.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptHashTest {

    @Test
    void printHashInfo() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String existingHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lH51";

        String[] candidates = {"hypepia123", "password", "secret", "admin", "test", "1234", "hypepia"};
        System.out.println("=== 기존 해시 검증 ===");
        for (String pw : candidates) {
            System.out.println(pw + " -> " + encoder.matches(pw, existingHash));
        }

        System.out.println("\n=== 새 해시 생성 ===");
        System.out.println("hypepia123: " + encoder.encode("hypepia123"));
    }
}
