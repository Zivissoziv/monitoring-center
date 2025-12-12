package com.example.monitoring.config;

import com.example.monitoring.job.MetricCollectionJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz configuration for scheduled jobs
 * Alert checking is now event-driven, so only metric collection job remains
 */
@Configuration
public class QuartzConfig {
    
    /**
     * Metric collection job - runs every 5 seconds to check if metrics need collection
     * Actual collection interval is determined by metric definition
     */
    @Bean
    public JobDetail metricCollectionJobDetail() {
        return JobBuilder.newJob(MetricCollectionJob.class)
                .withIdentity("metricCollectionJob")
                .withDescription("Check and collect metrics based on configured intervals")
                .storeDurably()
                .build();
    }
    
    @Bean
    public Trigger metricCollectionTrigger() {
        // Run every 5 seconds to check metrics (actual collection depends on interval)
        return TriggerBuilder.newTrigger()
                .forJob(metricCollectionJobDetail())
                .withIdentity("metricCollectionTrigger")
                .withDescription("Trigger for metric collection check")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(5)
                        .repeatForever())
                .build();
    }
    
    // AlertCheckJob removed - alert processing is now event-driven
    // Alerts are checked in real-time when metrics are collected via MetricCollectedEvent
}
