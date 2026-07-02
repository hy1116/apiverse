package com.hypepia.apiverse.admin.inquiry;

import com.hypepia.apiverse.core.entity.Inquiry;
import com.hypepia.apiverse.core.repository.InquiryRepository;
import com.hypepia.apiverse.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
public class InquiryAdminController {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    private Mono<Void> requireAdmin(Long uid) {
        return userRepository.findById(uid)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)))
                .flatMap(user -> "ADMIN".equals(user.getRole())
                        ? Mono.empty()
                        : Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)));
    }

    @GetMapping
    public Flux<Inquiry> list(@AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMapMany(uid -> inquiryRepository.findAllByOrderByCreatedAtDesc());
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Inquiry>> detail(@PathVariable Long id,
                                                @AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMap(uid -> inquiryRepository.findById(id))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/{id}/answer")
    public Mono<ResponseEntity<Inquiry>> answer(@PathVariable Long id,
                                                @RequestBody AnswerRequest req,
                                                @AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> requireAdmin(uid).thenReturn(uid))
                .flatMap(uid -> inquiryRepository.findById(id)
                        .flatMap(inq -> inquiryRepository.save(inq.toBuilder()
                                .answer(req.answer())
                                .status("ANSWERED")
                                .answeredAt(LocalDateTime.now())
                                .build())))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
