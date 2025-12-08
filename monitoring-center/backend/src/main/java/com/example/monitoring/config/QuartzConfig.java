package com.example.monitoring.config;

import com.example.monitoring.job.AlertCheckJob;
import com.example.monitoring.job.MetricCollectionJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quartz configuration for scheduled jobs
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
    
    /**
     * Alert check job - runs every 10 seconds
     */
    @Bean
    public JobDetail alertCheckJobDetail() {
        return JobBuilder.newJob(AlertCheckJob.class)
                .withIdentity("alertCheckJob")
                .withDescription("Check alerts for triggered conditions")
                .storeDurably()
                .build();
    }
    
    @Bean
    public Trigger alertCheckTrigger() {
        // Run every 10 seconds
        return TriggerBuilder.newTrigger()
                .forJob(alertCheckJobDetail())
                .withIdentity("alertCheckTrigger")
                .withDescription("Trigger for alert checking")
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(10)
                        .repeatForever())
                .build();
    }
}
