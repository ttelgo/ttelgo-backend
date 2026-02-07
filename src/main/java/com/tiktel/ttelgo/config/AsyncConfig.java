package com.tiktel.ttelgo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for async processing and scheduled jobs
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    // Configuration is in application.yml
    // spring.task.execution and spring.task.scheduling
}

