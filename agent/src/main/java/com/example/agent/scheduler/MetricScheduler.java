package com.example.agent.scheduler;

import com.example.agent.dto.MetricCollectionConfig;
import com.example.agent.service.MetricCollectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scheduler for metric collection
 * Checks every second which metrics need to be collected based on their intervals
 */
@Slf4j
@Component
public class MetricScheduler {
    
    @Autowired
    private MetricCollectionService metricCollectionService;
    
    // Track last collection time for each metric
    private final Map<String, Long> lastCollectionTimes = new ConcurrentHashMap<>();
    
    /**
     * Check and collect metrics every second
     */
    @Scheduled(fixedRate = 1000)
    public void checkAndCollectMetrics() {
        long currentTime = System.currentTimeMillis();
        
        for (MetricCollectionConfig config : metricCollectionService.getConfigs()) {
            String metricName = config.getMetricName();
            Integer interval = config.getCollectionInterval();
            
            if (interval == null || interval <= 0) {
                continue;
            }
            
            // Check if it's time to collect this metric
            Long lastTime = lastCollectionTimes.get(metricName);
            if (lastTime == null || (currentTime - lastTime) >= interval * 1000L) {
                log.debug("Time to collect metric: {} (interval: {}s)", metricName, interval);
                
                // Collect metric asynchronously
                try {
                    metricCollectionService.collectMetric(metricName);
                    lastCollectionTimes.put(metricName, currentTime);
                } catch (Exception e) {
                    log.error("Error collecting metric {}: {}", metricName, e.getMessage());
                }
            }
        }
    }
    
    /**
     * Reset collection time for a specific metric
     */
    public void resetMetric(String metricName) {
        lastCollectionTimes.remove(metricName);
        log.info("Reset collection time for metric: {}", metricName);
    }
    
    /**
     * Reset all collection times
     */
    public void resetAll() {
        lastCollectionTimes.clear();
        log.info("Reset all collection times");
    }
}
