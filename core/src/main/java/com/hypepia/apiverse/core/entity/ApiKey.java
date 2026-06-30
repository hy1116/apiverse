package com.hypepia.apiverse.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("api_keys")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("api_product_id")
    private Long apiProductId;

    @Column("api_key_value")
    private String apiKeyValue;

    @Column("white_list_ip")
    private String whiteListIp;

    @Builder.Default
    @Column("monthly_quota")
    private Integer monthlyQuota = -1;

    @Builder.Default
    @Column("used_quota")
    private Integer usedQuota = 0;

    @Builder.Default
    @Column("is_active")
    private Boolean isActive = true;

    @Column("created_at")
    private LocalDateTime createdAt;
}
