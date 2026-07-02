package com.hypepia.apiverse.core.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    // 게이트웨이 호출 경로에 쓰이는 슬러그 (/gateway/{code}/**) — DB PK를 외부에 노출하지 않기 위함
    private String code;

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

    // 업스트림 응답 형식 — JSON/XML/TEXT 중 하나 (프론트에서 안내용으로만 사용, 실제 파싱/변환은 하지 않음)
    @Builder.Default
    @Column("response_type")
    private String responseType = "JSON";

    @JsonRawValue
    @Column("spec_json")
    private String specJson;

    // 업스트림(실제 외부 API)이 자체 인증키를 요구하는 경우에만 사용 — 공개 엔드포인트로는 절대 노출 금지
    @JsonIgnore
    @Column("upstream_api_key")
    private String upstreamApiKey;

    // 업스트림 키 주입 위치. 형식: "header:{헤더명}" 또는 "query:{쿼리파라미터명}" (예: "query:serviceKey")
    @JsonIgnore
    @Column("upstream_key_param")
    private String upstreamKeyParam;
}
