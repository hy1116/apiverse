package com.hypepia.apiverse.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.hypepia.apiverse")
public class ApiverseGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiverseGatewayApplication.class, args);
    }
}
