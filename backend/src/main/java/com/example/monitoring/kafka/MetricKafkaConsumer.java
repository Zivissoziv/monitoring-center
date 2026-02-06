package com.example.monitoring.kafka;

import com.example.monitoring.dto.MetricData;
import com.example.monitoring.entity.Metric;
import com.example.monitoring.service.MetricService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Kafka consumer for receiving metrics from agents
 */
@Slf4j
@Component
public class MetricKafkaConsumer {
    
    @Autowired
    private MetricService metricService;
    
    /**
     * Consume metric data from Kafka topic
     */
    @KafkaListener(topics = "monitoring-metrics", groupId = "monitoring-backend")
    public void consumeMetric(@Payload MetricData metricData,
                              @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                              @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            log.info("Received metric from Kafka [partition={}, offset={}]: agentId={}, metricType={}, value={}", 
                    partition, offset, metricData.getAgentId(), metricData.getMetricType(), metricData.getMetricValue());
            
            // Convert to Metric entity
            Metric metric = new Metric();
            metric.setAgentId(metricData.getAgentId());
            metric.setMetricType(metricData.getMetricType());
            metric.setValue(metricData.getMetricValue());
            metric.setTextValue(metricData.getTextValue());
            // Convert Long timestamp (millis) to LocalDateTime
            if (metricData.getTimestamp() != null) {
                metric.setTimestamp(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(metricData.getTimestamp()), 
                    ZoneId.systemDefault()));
            } else {
                metric.setTimestamp(LocalDateTime.now());
            }
            
            // Save to database and trigger alert check
            metricService.saveMetric(metric);
            
            log.debug("Metric saved and processed successfully");
        } catch (Exception e) {
            log.error("Error processing metric from Kafka [partition={}, offset={}]: {}", 
                    partition, offset, e.getMessage(), e);
            // Don't rethrow - this allows consumer to continue with next message
        }
    }
}
