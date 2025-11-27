-- Agent Table
CREATE TABLE IF NOT EXISTS agents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    ip VARCHAR(50) NOT NULL,
    port INT NOT NULL,
    status VARCHAR(50) DEFAULT 'INACTIVE'
);

-- Metric Table
CREATE TABLE IF NOT EXISTS metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    metric_type VARCHAR(255) NOT NULL,
    metric_value DOUBLE NOT NULL,
    timestamp BIGINT NOT NULL
);

-- Alert Rule Table
CREATE TABLE IF NOT EXISTS alert_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    agent_id BIGINT,
    metric_type VARCHAR(255) NOT NULL,
    condition VARCHAR(10) NOT NULL,
    threshold DOUBLE NOT NULL,
    severity VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE
);

-- Create Indexes
CREATE INDEX IF NOT EXISTS idx_metrics_agent_id ON metrics(agent_id);
CREATE INDEX IF NOT EXISTS idx_metrics_timestamp ON metrics(timestamp);
CREATE INDEX IF NOT EXISTS idx_alert_rules_enabled ON alert_rules(enabled);