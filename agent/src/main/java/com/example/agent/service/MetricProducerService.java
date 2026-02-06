package com.example.agent.service;

import com.example.agent.dto.MetricData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for sending metrics to Kafka
 */
@Slf4j
@Service
public class MetricProducerService {
    
    private static final String METRIC_TOPIC = "monitoring-metrics";
    
    @Autowired
    private KafkaTemplate<String, MetricData> kafkaTemplate;
    
    /**
     * Send metric data to Kafka topic
     */
    public void sendMetric(MetricData metricData) {
        try {
            log.info("Sending metric to Kafka: agentId={}, metricType={}, value={}", 
                    metricData.getAgentId(), metricData.getMetricType(), metricData.getMetricValue());
            
            kafkaTemplate.send(METRIC_TOPIC, metricData.getAgentId(), metricData);
            
            log.debug("Metric sent successfully to Kafka");
        } catch (Exception e) {
            log.error("Failed to send metric to Kafka: {}", e.getMessage(), e);
        }
    }
}
