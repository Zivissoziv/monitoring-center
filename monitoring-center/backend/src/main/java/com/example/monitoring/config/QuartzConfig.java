package com.example.monitoring.config;

import org.springframework.context.annotation.Configuration;

/**
 * Quartz configuration for scheduled jobs
 * 
 * Metric collection is now handled by agents directly
 * Alert checking is event-driven when metrics are saved
 * All background jobs have been removed
 */
@Configuration
public class QuartzConfig {
    // All jobs removed - monitoring is now event-driven via Kafka
}
