package com.hypepia.apiverse.gateway.product;

import com.hypepia.apiverse.core.entity.ApiProduct;
import com.hypepia.apiverse.core.repository.ApiKeyRepository;
import com.hypepia.apiverse.core.repository.ApiProductRepository;
import com.hypepia.apiverse.core.repository.UserRepository;
import com.hypepia.apiverse.gateway.config.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ApiProductController {

    private final ApiProductRepository apiProductRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;

    @GetMapping
    public Flux<ApiProduct> listProducts() {
        return apiProductRepository.findAllByIsActiveTrueOrderByIsPremiumAsc();
    }

    @GetMapping("/{id}")
    public Mono<ApiProduct> getProduct(@PathVariable Long id) {
        return apiProductRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    // 로그인 상태이면 해당 유저의 키를 반환, 비로그인이면 {}
    @GetMapping("/{id}/my-key")
    public Mono<Map<String, String>> getMyKey(@PathVariable Long id) {
        return SecurityUtils.currentUserIdOrEmpty()
                .flatMap(userId -> apiKeyRepository.findByUserIdAndApiProductId(userId, id))
                .map(key -> Map.of("apiKeyValue", key.getApiKeyValue()))
                .defaultIfEmpty(Map.of());
    }

    // 인증된 사용자라면 누구나 등록 가능 — 승인 전까지 is_active = false
    @PostMapping
    public Mono<ResponseEntity<ApiProduct>> register(@RequestBody RegisterProductRequest req,
                                                     @AuthenticationPrincipal Mono<Long> principal) {
        if (req.name() == null || req.name().isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        if (req.baseUrl() == null || req.baseUrl().isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return principal.flatMap(uid -> generateUniqueCode(req.name().trim(), req.baseUrl().trim())
                .flatMap(code -> {
                    ApiProduct product = ApiProduct.builder()
                            .name(req.name().trim())
                            .code(code)
                            .description(req.description())
                            .baseUrl(req.baseUrl().trim())
                            .category(req.category())
                            .callsPerSec(req.callsPerSec() != null ? req.callsPerSec() : 5)
                            .responseType(req.responseType() != null && !req.responseType().isBlank() ? req.responseType() : "JSON")
                            .isPremium(Boolean.TRUE.equals(req.isPremium()))
                            .isActive(false)
                            .specJson(req.specJson())
                            .createdBy(uid)
                            .build();
                    return apiProductRepository.save(product)
                            .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved));
                }));
    }

    // code 생성 우선순위: base_url 도메인에서 의미 있는 부분 추출 → 실패 시 상품명 슬러그
    // /gateway/{code}/** 경로에 쓰임
    private Mono<String> generateUniqueCode(String name, String baseUrl) {
        String domainSlug = extractDomainSlug(baseUrl);
        String base = !domainSlug.isBlank() ? domainSlug : slugify(name);
        return findUniqueCode(base, 0);
    }

    private static final Set<String> GENERIC_SUBDOMAINS = Set.of("www", "api");

    // https://api.juso.go.kr/v2 → "juso" (www/api 같은 흔한 서브도메인은 건너뛰고 다음 라벨을 사용)
    static String extractDomainSlug(String baseUrl) {
        try {
            String host = new URI(baseUrl).getHost();
            if (host == null || host.isBlank()) {
                return "";
            }
            String[] labels = host.toLowerCase().split("\\.");
            int index = (labels.length > 1 && GENERIC_SUBDOMAINS.contains(labels[0])) ? 1 : 0;
            return labels[index];
        } catch (URISyntaxException e) {
            return "";
        }
    }

    private Mono<String> findUniqueCode(String base, int attempt) {
        String candidate = attempt == 0 ? base : base + "-" + (attempt + 1);
        return apiProductRepository.findByCode(candidate)
                .flatMap(existing -> findUniqueCode(base, attempt + 1))
                .switchIfEmpty(Mono.just(candidate));
    }

    private static String slugify(String name) {
        StringBuilder sb = new StringBuilder();
        boolean lastHyphen = false;
        for (char c : name.toLowerCase().toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
                lastHyphen = false;
            } else if (!lastHyphen && !sb.isEmpty()) {
                sb.append('-');
                lastHyphen = true;
            }
        }
        String slug = sb.toString();
        if (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }
        return slug.isEmpty() ? "api-" + UUID.randomUUID().toString().substring(0, 8) : slug;
    }

    // 내가 등록한 상품 목록 (승인 대기/승인 상태 확인용)
    @GetMapping("/my")
    public Flux<ApiProduct> listMine(@AuthenticationPrincipal Mono<Long> principal) {
        return principal.flatMapMany(apiProductRepository::findAllByCreatedByOrderByIdDesc);
    }

    // ADMIN 전용 — 승인 대기 중인 상품 목록
    @GetMapping("/pending")
    public Mono<ResponseEntity<Flux<ApiProduct>>> listPending(@AuthenticationPrincipal Mono<Long> principal) {
        return principal
                .flatMap(uid -> userRepository.findById(uid)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED))))
                .map(user -> {
                    if (!"ADMIN".equals(user.getRole())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                    }
                    return ResponseEntity.ok(apiProductRepository.findAllByIsActiveFalseOrderByIdDesc());
                });
    }

    // ADMIN 전용 — 상품 승인
    @PatchMapping("/{id}/approve")
    public Mono<ResponseEntity<ApiProduct>> approve(@PathVariable Long id,
                                                    @AuthenticationPrincipal Mono<Long> principal) {
        return principal
                .flatMap(uid -> userRepository.findById(uid)
                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED))))
                .flatMap(user -> {
                    if (!"ADMIN".equals(user.getRole())) {
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
                    }
                    return apiProductRepository.findById(id)
                            .flatMap(p -> apiProductRepository.save(p.toBuilder().isActive(true).build()))
                            .map(ResponseEntity::ok)
                            .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                });
    }
}
