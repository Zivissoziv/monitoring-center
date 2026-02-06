-- ========================================
-- System Initial Data: Menus, Roles, Users
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

INSERT INTO sys_menus (menu_code, menu_name, icon, sort_order, enabled)
SELECT 'apps', '应用管理', 'Grid', 10, TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_menus WHERE menu_code = 'apps');

-- Insert default applications
INSERT INTO sys_apps (app_code, app_name, description, enabled, created_at)
SELECT 'CORE', '核心应用', '系统核心应用', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_apps WHERE app_code = 'CORE');

INSERT INTO sys_apps (app_code, app_name, description, enabled, created_at)
SELECT 'DEMO', '演示应用', '演示测试用应用', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM sys_apps WHERE app_code = 'DEMO');

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

-- Assign DEMO app to demo user
INSERT INTO sys_user_apps (user_id, app_code)
SELECT 
    (SELECT id FROM sys_users WHERE username = 'demo' LIMIT 1),
    'DEMO'
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user_apps 
    WHERE user_id = (SELECT id FROM sys_users WHERE username = 'demo' LIMIT 1)
    AND app_code = 'DEMO'
);
