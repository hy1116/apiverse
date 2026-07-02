package com.hypepia.apiverse.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

// 게이트웨이(/gateway/**) 전역 접근을 차단할 IP 목록 — API 키/상품과 무관하게 ProxyService에서 최우선으로 검사
@Table("blocked_ips")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockedIp {

    @Id
    private Long id;

    @Column("ip_address")
    private String ipAddress;

    private String reason;

    @Builder.Default
    @Column("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
