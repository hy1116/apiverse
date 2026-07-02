package com.hypepia.apiverse.admin.user;

import com.hypepia.apiverse.core.entity.User;
import com.hypepia.apiverse.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserRepository userRepository;

    private Mono<Void> requireAdmin(Long uid) {
        return userRepository.findById(uid)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)))
                .flatMap(user -> "ADMIN".equals(user.getRole())
                        ? Mono.empty()
                        : Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)));
    }

    @GetMapping
    public Flux<Map<String, Object>> list(@AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMapMany(uid -> userRepository.findAllByOrderByIdDesc())
                .map(UserAdminController::toUserMap);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Map<String, Object>>> detail(@PathVariable Long id,
                                                            @AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMap(uid -> userRepository.findById(id))
                .map(user -> ResponseEntity.ok(toUserMap(user)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PatchMapping("/{id}/tier")
    public Mono<ResponseEntity<Map<String, Object>>> updateTier(@PathVariable Long id,
                                                                 @RequestBody UpdateTierRequest req,
                                                                 @AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMap(uid -> userRepository.findById(id))
                .flatMap(user -> {
                    user.setTier(req.tier());
                    return userRepository.save(user);
                })
                .map(user -> ResponseEntity.ok(toUserMap(user)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    private static Map<String, Object> toUserMap(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          user.getId());
        m.put("email",       user.getEmail());
        m.put("companyName", user.getCompanyName());
        m.put("phone",       user.getPhone());
        m.put("tier",        user.getTier());
        m.put("role",        user.getRole());
        m.put("createdAt",   user.getCreatedAt());
        return m;
    }
}
