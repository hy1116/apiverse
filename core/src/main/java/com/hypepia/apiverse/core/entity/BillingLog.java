package com.hypepia.apiverse.core.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("billing_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingLog {

    @Id
    private Long id;

    @Column("api_key_value")
    private String apiKeyValue;

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
