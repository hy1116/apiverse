package com.hypepia.apiverse.gateway.auth;

import com.hypepia.apiverse.core.entity.User;
import com.hypepia.apiverse.core.repository.UserRepository;
import com.hypepia.apiverse.gateway.config.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @GetMapping("/check-email")
    public Mono<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        return userRepository.findByEmail(email)
                .map(user -> Map.of("available", false))
                .defaultIfEmpty(Map.of("available", true));
    }

    @PostMapping("/signup")
    public Mono<Map<String, Object>> signup(@RequestBody Map<String, String> body) {
        String email       = body.getOrDefault("email", "").trim();
        String password    = body.getOrDefault("password", "");
        String companyName = body.getOrDefault("companyName", "");
        String phone       = body.getOrDefault("phone", "");

        if (email.isEmpty() || password.isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일과 비밀번호는 필수입니다."));
        }

        return userRepository.findByEmail(email)
                .flatMap(existing -> Mono.<Map<String, Object>>error(
                        new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.")))
                .switchIfEmpty(Mono.defer(() -> {
                    User user = User.builder()
                            .email(email)
                            .passwordHash(passwordEncoder.encode(password))
                            .companyName(companyName.isEmpty() ? null : companyName)
                            .phone(phone.isEmpty() ? null : phone)
                            .tier("FREE")
                            .build();
                    return userRepository.save(user)
                            .<Map<String, Object>>map(saved -> Map.of(
                                    "id",          saved.getId(),
                                    "email",       saved.getEmail(),
                                    "companyName", saved.getCompanyName() != null ? saved.getCompanyName() : "개인",
                                    "tier",        saved.getTier(),
                                    "token",       jwtUtils.generateToken(saved.getId())
                            ));
                }));
    }

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
                        return Mono.<Map<String, Object>>error(
                                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
                    }
                    return Mono.<Map<String, Object>>just(Map.of(
                            "id",          user.getId(),
                            "email",       user.getEmail(),
                            "companyName", user.getCompanyName() != null ? user.getCompanyName() : "개인",
                            "tier",        user.getTier() != null ? user.getTier() : "FREE",
                            "token",       jwtUtils.generateToken(user.getId())
                    ));
                });
    }
}
