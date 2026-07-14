package com.hypepia.apiverse.eventconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.hypepia.apiverse")
public class EventConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EventConsumerApplication.class, args);
    }
}
