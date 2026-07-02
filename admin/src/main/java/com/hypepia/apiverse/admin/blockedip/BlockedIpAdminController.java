package com.hypepia.apiverse.admin.blockedip;

import com.hypepia.apiverse.core.entity.BlockedIp;
import com.hypepia.apiverse.core.repository.BlockedIpRepository;
import com.hypepia.apiverse.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/blocked-ips")
@RequiredArgsConstructor
public class BlockedIpAdminController {

    private final BlockedIpRepository blockedIpRepository;
    private final UserRepository userRepository;

    private Mono<Void> requireAdmin(Long uid) {
        return userRepository.findById(uid)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)))
                .flatMap(user -> "ADMIN".equals(user.getRole())
                        ? Mono.empty()
                        : Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)));
    }

    @GetMapping
    public Flux<BlockedIp> list(@AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMapMany(uid -> blockedIpRepository.findAllByOrderByIdDesc());
    }

    @PostMapping
    public Mono<ResponseEntity<BlockedIp>> add(@RequestBody AddBlockedIpRequest req,
                                               @AuthenticationPrincipal Mono<Long> principal) {
        if (req.ipAddress() == null || req.ipAddress().isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMap(uid -> blockedIpRepository.findByIpAddress(req.ipAddress().trim())
                        .flatMap(existing -> Mono.<ResponseEntity<BlockedIp>>error(
                                new ResponseStatusException(HttpStatus.CONFLICT, "이미 차단된 IP입니다.")))
                        .switchIfEmpty(Mono.defer(() -> blockedIpRepository.save(
                                        BlockedIp.builder()
                                                .ipAddress(req.ipAddress().trim())
                                                .reason(req.reason())
                                                .build())
                                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved)))));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> remove(@PathVariable Long id,
                                             @AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMap(uid -> blockedIpRepository.deleteById(id))
                .thenReturn(ResponseEntity.noContent().<Void>build());
    }
}
