package com.hypepia.apiverse.admin.auth;

import com.hypepia.apiverse.admin.config.JwtUtils;
import com.hypepia.apiverse.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public Mono<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String email    = body.getOrDefault("email", "").trim();
        String password = body.getOrDefault("password", "");
        if (email.isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일을 입력해주세요."));
        }
        return userRepository.findByEmail(email)
                .switchIfEmpty(Mono.error(
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                        return Mono.error(
                                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
                    }
                    if (!"ADMIN".equals(user.getRole())) {
                        return Mono.error(
                                new ResponseStatusException(HttpStatus.FORBIDDEN, "관리자 계정이 아닙니다."));
                    }
                    return Mono.just(Map.of(
                            "id",          user.getId(),
                            "email",       user.getEmail(),
                            "companyName", user.getCompanyName() != null ? user.getCompanyName() : "",
                            "tier",        user.getTier(),
                            "role",        user.getRole(),
                            "token",       jwtUtils.generateToken(user.getId())
                    ));
                });
    }
}
