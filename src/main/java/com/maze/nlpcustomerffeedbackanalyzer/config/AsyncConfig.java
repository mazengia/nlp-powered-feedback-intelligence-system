package com.maze.nlpcustomerffeedbackanalyzer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Async configuration is provided via application.yml properties:
    // spring.task.execution.pool.core-size and max-size
}

