package com.portx.routesystem.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * AppConfig - Application bean definitions.
 * Configures RestTemplate with 2.5s connect & read timeouts.
 */
@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(2500))
                .setReadTimeout(Duration.ofMillis(2500))
                .build();
    }
}
