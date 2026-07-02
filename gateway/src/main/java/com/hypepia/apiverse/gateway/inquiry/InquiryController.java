package com.hypepia.apiverse.gateway.inquiry;

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
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    @PostMapping
    public Mono<Inquiry> submit(@RequestBody InquiryRequest req,
                                @AuthenticationPrincipal Mono<Long> principal) {
        if (req.title() == null || req.title().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "제목을 입력해주세요"));
        }
        if (req.content() == null || req.content().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "내용을 입력해주세요"));
        }
        return principal.flatMap(uid -> inquiryRepository.save(Inquiry.builder()
                .userId(uid)
                .title(req.title().trim())
                .content(req.content().trim())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build()));
    }

    @GetMapping
    public Flux<Inquiry> list(@AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMapMany(inquiryRepository::findByUserIdOrderByCreatedAtDesc);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<Inquiry>> detail(@PathVariable Long id,
                                                @AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> inquiryRepository.findById(id)
                .<ResponseEntity<Inquiry>>flatMap(inq -> {
                    if (!inq.getUserId().equals(uid)) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                    }
                    return Mono.just(ResponseEntity.ok(inq));
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> delete(@PathVariable Long id,
                                             @AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMap(uid -> inquiryRepository.findById(id)
                .<ResponseEntity<Void>>flatMap(inq -> {
                    if (!inq.getUserId().equals(uid)) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                    }
                    return inquiryRepository.deleteById(id)
                            .then(Mono.just(ResponseEntity.status(HttpStatus.NO_CONTENT).build()));
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
    }

    @PostMapping("/{id}/answer")
    public Mono<ResponseEntity<Inquiry>> answer(@PathVariable Long id,
                                                @RequestBody AnswerRequest req,
                                                @AuthenticationPrincipal Mono<Long> principal) {
        return principal
                .flatMap(uid -> userRepository.findById(uid)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED))))
                .flatMap(user -> {
                    if (!"ADMIN".equals(user.getRole())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                    }
                    return inquiryRepository.findById(id)
                            .flatMap(inq -> inquiryRepository.save(inq.toBuilder()
                                    .answer(req.answer())
                                    .status("ANSWERED")
                                    .answeredAt(LocalDateTime.now())
                                    .build()))
                            .map(ResponseEntity::ok)
                            .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                });
    }
}
