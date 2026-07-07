package com.hypepia.apiverse.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

// gateway 앱 전체(로그인/상품/키/문의 등 REST API)에 대한 접근 로그.
// /gateway/** 프록시 호출은 api_key_value 기준으로 billing_logs에 별도 기록되므로 여기서는 제외한다.
@Table("access_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccessLog {

    @Id
    private Long id;

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
