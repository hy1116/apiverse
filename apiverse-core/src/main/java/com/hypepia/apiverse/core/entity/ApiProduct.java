package com.hypepia.apiverse.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("api_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiProduct {

    @Id
    private Long id;

    private String name;

    private String description;

    @Column("base_url")
    private String baseUrl;

    @Builder.Default
    @Column("is_premium")
    private Boolean isPremium = false;

    @Builder.Default
    @Column("is_active")
    private Boolean isActive = true;
}
