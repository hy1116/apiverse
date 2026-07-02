package com.hypepia.apiverse.gateway.profile;

import com.hypepia.apiverse.core.entity.User;
import com.hypepia.apiverse.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// 경로가 /api/auth/**에 속하지 않도록 /api/profile을 쓴다 — SecurityConfig의 /api/auth/** permitAll 규칙에
// 걸리지 않고 anyExchange().authenticated() catch-all로 자연스럽게 인증이 강제되게 하기 위함.
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;

    @GetMapping
    public Mono<Map<String, Object>> me(@AuthenticationPrincipal Mono<Long> principal) {
        return principal
                .flatMap(uid -> userRepository.findById(uid)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED))))
                .map(ProfileController::toProfileMap);
    }

    @PatchMapping
    public Mono<Map<String, Object>> update(@RequestBody UpdateProfileRequest req,
                                            @AuthenticationPrincipal Mono<Long> principal) {
        return principal
                .flatMap(uid -> userRepository.findById(uid)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED))))
                .flatMap(user -> {
                    if (req.companyName() != null) {
                        user.setCompanyName(req.companyName().isBlank() ? null : req.companyName().trim());
                    }
                    if (req.phone() != null) {
                        user.setPhone(req.phone().isBlank() ? null : req.phone().trim());
                    }
                    user.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(user);
                })
                .map(ProfileController::toProfileMap);
    }

    private static Map<String, Object> toProfileMap(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          user.getId());
        m.put("email",       user.getEmail());
        m.put("companyName", user.getCompanyName());
        m.put("phone",       user.getPhone());
        m.put("tier",        user.getTier());
        m.put("createdAt",   user.getCreatedAt());
        m.put("updatedAt",   user.getUpdatedAt());
        return m;
    }
}
