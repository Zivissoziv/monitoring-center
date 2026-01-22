-- ========================================
-- DML: Initial Data Population
-- ========================================

-- Insert default metric definitions
INSERT INTO metric_definitions (metric_name, display_name, description, metric_type, collection_command, collection_interval, processing_rule, unit, enabled, created_at, updated_at)
SELECT 'CPU', 'CPU Usage', 'CPU usage percentage', 'NUMERIC', 'top -bn1 | awk ''/Cpu/{print 100-$8}''', 60, NULL, '%', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM metric_definitions WHERE metric_name = 'CPU');

INSERT INTO metric_definitions (metric_name, display_name, description, metric_type, collection_command, collection_interval, processing_rule, unit, enabled, created_at, updated_at)
SELECT 'MEMORY', 'Memory Usage', 'Memory usage percentage', 'NUMERIC', 'free | awk ''/Mem/{printf "%.2f", $3/$2*100}''', 60, NULL, '%', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM metric_definitions WHERE metric_name = 'MEMORY');

INSERT INTO metric_definitions (metric_name, display_name, description, metric_type, collection_command, collection_interval, processing_rule, unit, enabled, created_at, updated_at)
SELECT 'PORT_8088', 'Port 8088 Status', 'Check if port 8088 is listening', 'BOOLEAN', 'netstat -tuln | grep :8088 > /dev/null && echo 1 || echo 0', 30, NULL, '', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM metric_definitions WHERE metric_name = 'PORT_8088');

-- Insert alert rule for port 8088 monitoring (boolean type)
INSERT INTO alert_rules (name, agent_id, metric_type, condition, threshold, threshold_text, severity, alert_message, enabled)
SELECT 'Port 8088 Down Alert', NULL, 'PORT_8088', 'EQUALS', NULL, 'false', 'HIGH', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM alert_rules WHERE name = 'Port 8088 Down Alert');

-- Insert emergency knowledge base 1: Quick Restart Solution
INSERT INTO emergency_knowledge (title, description, created_at, updated_at)
SELECT 
    'Port 8088 Quick Restart Solution', 
    'Immediate actions to restart the service on port 8088 and verify its status',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM emergency_knowledge WHERE title = 'Port 8088 Quick Restart Solution');

-- Insert emergency knowledge base 2: Diagnostic Investigation
INSERT INTO emergency_knowledge (title, description, created_at, updated_at)
SELECT 
    'Port 8088 Diagnostic Investigation', 
    'Detailed diagnostic steps to investigate why port 8088 is not responding',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM emergency_knowledge WHERE title = 'Port 8088 Diagnostic Investigation');

-- Link both knowledge bases to the Port 8088 Down Alert rule
INSERT INTO alert_rule_emergency (alert_rule_id, emergency_knowledge_id, created_at)
SELECT 
    (SELECT id FROM alert_rules WHERE name = 'Port 8088 Down Alert' LIMIT 1),
    (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Quick Restart Solution' LIMIT 1),
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM alert_rule_emergency 
    WHERE alert_rule_id = (SELECT id FROM alert_rules WHERE name = 'Port 8088 Down Alert' LIMIT 1)
    AND emergency_knowledge_id = (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Quick Restart Solution' LIMIT 1)
);

INSERT INTO alert_rule_emergency (alert_rule_id, emergency_knowledge_id, created_at)
SELECT 
    (SELECT id FROM alert_rules WHERE name = 'Port 8088 Down Alert' LIMIT 1),
    (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Diagnostic Investigation' LIMIT 1),
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM alert_rule_emergency 
    WHERE alert_rule_id = (SELECT id FROM alert_rules WHERE name = 'Port 8088 Down Alert' LIMIT 1)
    AND emergency_knowledge_id = (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Diagnostic Investigation' LIMIT 1)
);

-- Insert emergency steps for Knowledge Base 1: Quick Restart Solution
-- Step 0: URL Jump to health check page
INSERT INTO emergency_steps (knowledge_id, step_order, step_type, description, jump_url, agent_id, depends_on, notes)
SELECT 
    (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Quick Restart Solution' LIMIT 1),
    0,
    'URL_JUMP',
    'Open service health check page',
    'http://localhost:8088/health',
    NULL,
    NULL,
    'Check the current health status of the service'
WHERE NOT EXISTS (
    SELECT 1 FROM emergency_steps 
    WHERE knowledge_id = (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Quick Restart Solution' LIMIT 1)
    AND step_order = 0
);

INSERT INTO emergency_steps (knowledge_id, step_order, step_type, description, linux_command, agent_id, depends_on, notes)
SELECT 
    (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Quick Restart Solution' LIMIT 1),
    1,
    'COMMAND',
    'Check current port 8088 status',
    'netstat -tuln | grep :8088',
    NULL,
    NULL,
    'Verify if the port is actually down'
WHERE NOT EXISTS (
    SELECT 1 FROM emergency_steps 
    WHERE knowledge_id = (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Quick Restart Solution' LIMIT 1)
    AND step_order = 1
);

INSERT INTO emergency_steps (knowledge_id, step_order, step_type, description, linux_command, agent_id, depends_on, notes)
SELECT 
    (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Quick Restart Solution' LIMIT 1),
    2,
    'COMMAND',
    'Find and kill the process using port 8088',
    'lsof -ti:8088 | xargs kill -9',
    NULL,
    NULL,
    'Force stop any process on port 8088'
WHERE NOT EXISTS (
    SELECT 1 FROM emergency_steps 
    WHERE knowledge_id = (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Quick Restart Solution' LIMIT 1)
    AND step_order = 2
);

INSERT INTO emergency_steps (knowledge_id, step_order, step_type, description, linux_command, agent_id, depends_on, notes)
SELECT 
    (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Quick Restart Solution' LIMIT 1),
    3,
    'COMMAND',
    'Restart the service',
    'systemctl restart myapp-8088',
    NULL,
    NULL,
    'Replace myapp-8088 with your actual service name'
WHERE NOT EXISTS (
    SELECT 1 FROM emergency_steps 
    WHERE knowledge_id = (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Quick Restart Solution' LIMIT 1)
    AND step_order = 3
);

INSERT INTO emergency_steps (knowledge_id, step_order, step_type, description, linux_command, agent_id, depends_on, notes)
SELECT 
    (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Quick Restart Solution' LIMIT 1),
    4, 'COMMAND', 'Verify port 8088 is listening again',
    'netstat -tuln | grep :8088',
    NULL,
    NULL,
    'Confirm the service is running normally'
WHERE NOT EXISTS (
    SELECT 1 FROM emergency_steps 
    WHERE knowledge_id = (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Quick Restart Solution' LIMIT 1)
    AND step_order = 4
);

-- Insert emergency steps for Knowledge Base 2: Diagnostic Investigation
INSERT INTO emergency_steps (knowledge_id, step_order, step_type, description, linux_command, agent_id, depends_on, notes)
SELECT 
    (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Diagnostic Investigation' LIMIT 1),
    1, 'COMMAND', 'Check service logs for errors',
    'journalctl -u myapp-8088 -n 50 --no-pager',
    NULL,
    NULL,
    'Review recent service logs for error messages'
WHERE NOT EXISTS (
    SELECT 1 FROM emergency_steps 
    WHERE knowledge_id = (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Diagnostic Investigation' LIMIT 1)
    AND step_order = 1
);

INSERT INTO emergency_steps (knowledge_id, step_order, step_type, description, linux_command, agent_id, depends_on, notes)
SELECT 
    (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Diagnostic Investigation' LIMIT 1),
    2, 'COMMAND', 'Check if port is blocked by firewall',
    'iptables -L -n | grep 8088',
    NULL,
    NULL,
    'Verify firewall rules are not blocking the port'
WHERE NOT EXISTS (
    SELECT 1 FROM emergency_steps 
    WHERE knowledge_id = (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Diagnostic Investigation' LIMIT 1)
    AND step_order = 2
);

INSERT INTO emergency_steps (knowledge_id, step_order, step_type, description, linux_command, agent_id, depends_on, notes)
SELECT 
    (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Diagnostic Investigation' LIMIT 1),
    3, 'COMMAND', 'Check system resource usage',
    'top -bn1 | head -20',
    NULL,
    NULL,
    'See if system resources are exhausted (CPU/Memory)'
WHERE NOT EXISTS (
    SELECT 1 FROM emergency_steps 
    WHERE knowledge_id = (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Diagnostic Investigation' LIMIT 1)
    AND step_order = 3
);

INSERT INTO emergency_steps (knowledge_id, step_order, step_type, description, linux_command, agent_id, depends_on, notes)
SELECT 
    (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Diagnostic Investigation' LIMIT 1),
    4, 'COMMAND', 'Check disk space availability',
    'df -h',
    NULL,
    NULL,
    'Ensure sufficient disk space is available'
WHERE NOT EXISTS (
    SELECT 1 FROM emergency_steps 
    WHERE knowledge_id = (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Diagnostic Investigation' LIMIT 1)
    AND step_order = 4
);

INSERT INTO emergency_steps (knowledge_id, step_order, step_type, description, linux_command, agent_id, depends_on, notes)
SELECT 
    (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Diagnostic Investigation' LIMIT 1),
    5, 'COMMAND', 'Review application configuration',
    'cat /etc/myapp/config.yml | grep -A 5 port',
    NULL,
    NULL,
    'Verify the application is configured to use port 8088'
WHERE NOT EXISTS (
    SELECT 1 FROM emergency_steps 
    WHERE knowledge_id = (SELECT id FROM emergency_knowledge WHERE title = 'Port 8088 Diagnostic Investigation' LIMIT 1)
    AND step_order = 5
);
