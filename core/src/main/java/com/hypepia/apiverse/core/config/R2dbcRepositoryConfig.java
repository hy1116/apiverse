package com.hypepia.apiverse.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories(basePackages = "com.hypepia.apiverse.core.repository")
public class R2dbcRepositoryConfig {
}
