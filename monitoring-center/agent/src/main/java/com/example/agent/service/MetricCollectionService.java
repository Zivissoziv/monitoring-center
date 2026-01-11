package com.example.agent.service;

import com.example.agent.config.AgentProperties;
import com.example.agent.dto.MetricCollectionConfig;
import com.example.agent.dto.MetricData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for collecting metrics on agent
 */
@Slf4j
@Service
public class MetricCollectionService {
    
    @Autowired
    private AgentProperties agentProperties;
    
    @Autowired
    private MetricProducerService metricProducerService;
    
    // Store metric collection configurations
    private final Map<String, MetricCollectionConfig> metricConfigs = new ConcurrentHashMap<>();
    
    /**
     * Update metric collection configurations
     */
    public void updateConfigs(List<MetricCollectionConfig> configs) {
        log.info("Updating metric collection configs, count: {}", configs.size());
        metricConfigs.clear();
        for (MetricCollectionConfig config : configs) {
            if (config.getEnabled()) {
                metricConfigs.put(config.getMetricName(), config);
                log.info("Loaded config: {} (interval: {}s)", config.getMetricName(), config.getCollectionInterval());
            }
        }
    }
    
    /**
     * Get all enabled metric configurations
     */
    public List<MetricCollectionConfig> getConfigs() {
        return new ArrayList<>(metricConfigs.values());
    }
    
    /**
     * Collect a specific metric
     */
    public void collectMetric(String metricName) {
        MetricCollectionConfig config = metricConfigs.get(metricName);
        if (config == null) {
            log.warn("Metric config not found: {}", metricName);
            return;
        }
        
        try {
            log.debug("Collecting metric: {}", metricName);
            
            // Execute collection command
            String command = config.getCollectionCommand();
            String result = executeCommand(command);
            
            if (result == null || result.trim().isEmpty()) {
                log.warn("Empty result for metric: {}", metricName);
                return;
            }
            
            // Create metric data
            MetricData metricData = new MetricData();
            metricData.setAgentId(agentProperties.getName());
            metricData.setMetricType(metricName);
            metricData.setTimestamp(System.currentTimeMillis());
            
            // Parse result based on metric type
            if ("NUMERIC".equals(config.getMetricType())) {
                try {
                    double value = Double.parseDouble(result.trim());
                    metricData.setMetricValue(value);
                } catch (NumberFormatException e) {
                    log.error("Failed to parse numeric value for {}: {}", metricName, result);
                    return;
                }
            } else if ("BOOLEAN".equals(config.getMetricType())) {
                String trimmed = result.trim();
                if ("1".equals(trimmed) || "true".equalsIgnoreCase(trimmed)) {
                    metricData.setMetricValue(1.0);
                    metricData.setTextValue("true");
                } else {
                    metricData.setMetricValue(0.0);
                    metricData.setTextValue("false");
                }
            } else {
                metricData.setTextValue(result.trim());
            }
            
            // Send to Kafka
            metricProducerService.sendMetric(metricData);
            
            log.info("Metric collected: {} = {}", metricName, 
                    metricData.getMetricValue() != null ? metricData.getMetricValue() : metricData.getTextValue());
            
        } catch (Exception e) {
            log.error("Error collecting metric {}: {}", metricName, e.getMessage(), e);
        }
    }
    
    /**
     * Execute shell command and return output
     */
    private String executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("Command exited with code {}: {}", exitCode, command);
            }
            
            return output.toString().trim();
        } catch (Exception e) {
            log.error("Failed to execute command: {}", command, e);
            return null;
        }
    }
}
