package com.example.monitoring.entity;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Custom metric definition with collection command and processing rules
 */
@Entity
@Table(name = "metric_definitions")
@Data
@NoArgsConstructor
public class MetricDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String metricName; // Unique identifier like "CPU", "MEMORY", "DISK_IO", etc.
    
    @Column(nullable = false)
    private String displayName; // Display name for UI
    
    @Column(length = 2000)
    private String description; // Description of the metric
    
    @Column(name = "metric_type")
    private String metricType = "NUMERIC"; // NUMERIC, BOOLEAN, STRING
    
    @Column(name = "collection_command", length = 2000, nullable = false)
    private String collectionCommand; // Shell command to collect metric
    
    @Column(name = "collection_interval")
    private int collectionInterval = 30; // Collection frequency in seconds
    
    @Column(name = "processing_rule", length = 2000)
    private String processingRule; // JavaScript expression to process command output
    
    @Column(name = "unit")
    private String unit = "%"; // Unit of measurement (%, MB, count, etc.)
    
    @Column(name = "enabled")
    private Boolean enabled = true;
    
    @Column(name = "created_at")
    private long createdAt;
    
    @Column(name = "updated_at")
    private long updatedAt;
    
    public MetricDefinition(String metricName, String displayName, String collectionCommand) {
        this.metricName = metricName;
        this.displayName = displayName;
        this.collectionCommand = collectionCommand;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}
