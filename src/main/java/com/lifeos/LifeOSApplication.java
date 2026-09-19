package com.lifeos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * LifeOS Main Application Entrypoint.
 * AI-Powered Personal Life Management & Knowledge Platform.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class LifeOSApplication {

    public static void main(String[] args) {
        SpringApplication.run(LifeOSApplication.class, args);
    }
}
