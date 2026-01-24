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

-- ========================================
-- Permission Management Initial Data
-- ========================================

-- Insert system menus (matching frontend App.vue menu items)
INSERT INTO sys_menus (menu_code, menu_name, icon, sort_order, enabled)
SELECT 'dashboard', '监控台', 'DataBoard', 1, TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_menus WHERE menu_code = 'dashboard');

INSERT INTO sys_menus (menu_code, menu_name, icon, sort_order, enabled)
SELECT 'agents', '代理管理', 'Monitor', 2, TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_menus WHERE menu_code = 'agents');

INSERT INTO sys_menus (menu_code, menu_name, icon, sort_order, enabled)
SELECT 'metrics', '监控指标', 'TrendCharts', 3, TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_menus WHERE menu_code = 'metrics');

INSERT INTO sys_menus (menu_code, menu_name, icon, sort_order, enabled)
SELECT 'metric-definitions', '指标定义', 'Setting', 4, TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_menus WHERE menu_code = 'metric-definitions');

INSERT INTO sys_menus (menu_code, menu_name, icon, sort_order, enabled)
SELECT 'alerts', '告警管理', 'Bell', 5, TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_menus WHERE menu_code = 'alerts');

INSERT INTO sys_menus (menu_code, menu_name, icon, sort_order, enabled)
SELECT 'emergency', '应急知识库', 'Reading', 6, TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_menus WHERE menu_code = 'emergency');

INSERT INTO sys_menus (menu_code, menu_name, icon, sort_order, enabled)
SELECT 'third-party', '第三方告警', 'Connection', 7, TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_menus WHERE menu_code = 'third-party');

INSERT INTO sys_menus (menu_code, menu_name, icon, sort_order, enabled)
SELECT 'users', '用户管理', 'User', 8, TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_menus WHERE menu_code = 'users');

INSERT INTO sys_menus (menu_code, menu_name, icon, sort_order, enabled)
SELECT 'roles', '角色管理', 'UserFilled', 9, TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_menus WHERE menu_code = 'roles');

-- Insert system roles
INSERT INTO sys_roles (role_code, role_name, description, created_at)
SELECT 'admin', '系统管理员', '拥有所有权限', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_roles WHERE role_code = 'admin');

INSERT INTO sys_roles (role_code, role_name, description, created_at)
SELECT 'user', '普通用户', '只能查看监控数据和告警', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_roles WHERE role_code = 'user');

-- Insert admin user (password: admin123, will be encoded at startup)
INSERT INTO sys_users (username, password, nickname, enabled, created_at, updated_at)
SELECT 'admin', 'TEMP_PASSWORD_WILL_BE_ENCODED', '系统管理员', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_users WHERE username = 'admin');

-- Insert demo user (password: demo123, will be encoded at startup)
INSERT INTO sys_users (username, password, nickname, enabled, created_at, updated_at)
SELECT 'demo', 'TEMP_PASSWORD_WILL_BE_ENCODED', '演示用户', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_users WHERE username = 'demo');

-- Assign admin role to admin user
INSERT INTO sys_user_roles (user_id, role_id)
SELECT 
    (SELECT id FROM sys_users WHERE username = 'admin' LIMIT 1),
    (SELECT id FROM sys_roles WHERE role_code = 'admin' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user_roles 
    WHERE user_id = (SELECT id FROM sys_users WHERE username = 'admin' LIMIT 1)
    AND role_id = (SELECT id FROM sys_roles WHERE role_code = 'admin' LIMIT 1)
);

-- Assign user role to demo user
INSERT INTO sys_user_roles (user_id, role_id)
SELECT 
    (SELECT id FROM sys_users WHERE username = 'demo' LIMIT 1),
    (SELECT id FROM sys_roles WHERE role_code = 'user' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user_roles 
    WHERE user_id = (SELECT id FROM sys_users WHERE username = 'demo' LIMIT 1)
    AND role_id = (SELECT id FROM sys_roles WHERE role_code = 'user' LIMIT 1)
);

-- Assign all menus to admin role
INSERT INTO sys_role_menus (role_id, menu_id)
SELECT 
    (SELECT id FROM sys_roles WHERE role_code = 'admin' LIMIT 1),
    id
FROM sys_menus
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menus 
    WHERE role_id = (SELECT id FROM sys_roles WHERE role_code = 'admin' LIMIT 1)
    AND menu_id = sys_menus.id
);

-- Assign limited menus to user role (dashboard, agents, metrics, alerts)
INSERT INTO sys_role_menus (role_id, menu_id)
SELECT 
    (SELECT id FROM sys_roles WHERE role_code = 'user' LIMIT 1),
    (SELECT id FROM sys_menus WHERE menu_code = 'dashboard' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menus 
    WHERE role_id = (SELECT id FROM sys_roles WHERE role_code = 'user' LIMIT 1)
    AND menu_id = (SELECT id FROM sys_menus WHERE menu_code = 'dashboard' LIMIT 1)
);

INSERT INTO sys_role_menus (role_id, menu_id)
SELECT 
    (SELECT id FROM sys_roles WHERE role_code = 'user' LIMIT 1),
    (SELECT id FROM sys_menus WHERE menu_code = 'agents' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menus 
    WHERE role_id = (SELECT id FROM sys_roles WHERE role_code = 'user' LIMIT 1)
    AND menu_id = (SELECT id FROM sys_menus WHERE menu_code = 'agents' LIMIT 1)
);

INSERT INTO sys_role_menus (role_id, menu_id)
SELECT 
    (SELECT id FROM sys_roles WHERE role_code = 'user' LIMIT 1),
    (SELECT id FROM sys_menus WHERE menu_code = 'metrics' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menus 
    WHERE role_id = (SELECT id FROM sys_roles WHERE role_code = 'user' LIMIT 1)
    AND menu_id = (SELECT id FROM sys_menus WHERE menu_code = 'metrics' LIMIT 1)
);

INSERT INTO sys_role_menus (role_id, menu_id)
SELECT 
    (SELECT id FROM sys_roles WHERE role_code = 'user' LIMIT 1),
    (SELECT id FROM sys_menus WHERE menu_code = 'alerts' LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menus 
    WHERE role_id = (SELECT id FROM sys_roles WHERE role_code = 'user' LIMIT 1)
    AND menu_id = (SELECT id FROM sys_menus WHERE menu_code = 'alerts' LIMIT 1)
);
