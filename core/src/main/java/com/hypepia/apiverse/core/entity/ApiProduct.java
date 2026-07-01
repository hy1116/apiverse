package com.hypepia.apiverse.core.entity;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("api_products")
@Data
@Builder(toBuilder = true)
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

    private String category;

    @Column("calls_per_sec")
    private Integer callsPerSec;

    @JsonRawValue
    @Column("spec_json")
    private String specJson;
}
