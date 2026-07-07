package com.hypepia.apiverse.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

// gateway/admin 앱의 REST API 전체에 대한 접근 로그 (source로 구분).
// /gateway/** 프록시 호출은 api_key_value 기준으로 billing_logs에 별도 기록되므로 여기서는 제외한다.
@Table("access_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessLog {

    @Id
    private Long id;

    // 'GATEWAY' | 'ADMIN' — 앱이 늘어나면 값만 추가하면 되므로 boolean 컬럼 대신 문자열 구분 컬럼을 쓴다.
    @Column("source")
    private String source;

    @Column("user_id")
    private Long userId;

    @Column("request_path")
    private String requestPath;

    @Column("http_method")
    private String httpMethod;

    @Column("response_status")
    private Integer responseStatus;

    @Column("client_ip")
    private String clientIp;

    @Column("request_time")
    private LocalDateTime requestTime;
}
