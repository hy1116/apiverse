package com.hypepia.apiverse.gateway.logging;

import com.hypepia.apiverse.core.repository.AccessLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

@Configuration
public class AccessLoggingConfig {

    @Bean
    public WebFilter accessLogWebFilter(AccessLogRepository accessLogRepository,
                                         @Value("${app.trust-forwarded-headers:false}") boolean trustForwardedHeaders) {
        return new AccessLogWebFilter(accessLogRepository, trustForwardedHeaders);
    }
}
