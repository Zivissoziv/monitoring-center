package com.example.monitoring.metric;

import javax.persistence.*;

/**
 * Custom metric definition with collection command and processing rules
 */
@Entity
@Table(name = "metric_definitions")
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
    
    @Column(name = "collection_command", length = 2000, nullable = false)
    private String collectionCommand; // Shell command to collect metric
    
    @Column(name = "collection_interval")
    private int collectionInterval = 30; // Collection frequency in seconds
    
    @Column(name = "processing_rule", length = 2000)
    private String processingRule; // JavaScript expression to process command output
    
    @Column(name = "unit")
    private String unit = "%"; // Unit of measurement (%, MB, count, etc.)
    
    @Column(name = "enabled")
    private boolean enabled = true;
    
    @Column(name = "created_at")
    private long createdAt;
    
    @Column(name = "updated_at")
    private long updatedAt;
    
    // Constructors
    public MetricDefinition() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public MetricDefinition(String metricName, String displayName, String collectionCommand) {
        this();
        this.metricName = metricName;
        this.displayName = displayName;
        this.collectionCommand = collectionCommand;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getMetricName() {
        return metricName;
    }
    
    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCollectionCommand() {
        return collectionCommand;
    }
    
    public void setCollectionCommand(String collectionCommand) {
        this.collectionCommand = collectionCommand;
    }
    
    public int getCollectionInterval() {
        return collectionInterval;
    }
    
    public void setCollectionInterval(int collectionInterval) {
        this.collectionInterval = collectionInterval;
    }
    
    public String getProcessingRule() {
        return processingRule;
    }
    
    public void setProcessingRule(String processingRule) {
        this.processingRule = processingRule;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
