# 批量更新package声明和import语句的PowerShell脚本

$basePath = "src\main\java\com\example\monitoring"

# 更新entity包中的package声明
Get-ChildItem -Path "$basePath\entity\*.java" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $content = $content -replace 'package com\.example\.monitoring\.(agent|alert|metric|emergency);', 'package com.example.monitoring.entity;'
    Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
    Write-Host "Updated package in: $($_.Name)"
}

# 更新repository包中的package声明
Get-ChildItem -Path "$basePath\repository\*.java" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $content = $content -replace 'package com\.example\.monitoring\.(agent|alert|metric|emergency);', 'package com.example.monitoring.repository;'
    Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
    Write-Host "Updated package in: $($_.Name)"
}

# 更新service包中的package声明
Get-ChildItem -Path "$basePath\service\*.java" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $content = $content -replace 'package com\.example\.monitoring\.(agent|alert|metric|emergency);', 'package com.example.monitoring.service;'
    Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
    Write-Host "Updated package in: $($_.Name)"
}

# 更新controller包中的package声明
Get-ChildItem -Path "$basePath\controller\*.java" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $content = $content -replace 'package com\.example\.monitoring\.(agent|alert|metric|emergency);', 'package com.example.monitoring.controller;'
    Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
    Write-Host "Updated package in: $($_.Name)"
}

# 更新所有Java文件的import语句
Get-ChildItem -Path "$basePath\*.java" -Recurse | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    
    # Entity imports
    $content = $content -replace 'import com\.example\.monitoring\.agent\.Agent;', 'import com.example.monitoring.entity.Agent;'
    $content = $content -replace 'import com\.example\.monitoring\.alert\.Alert;', 'import com.example.monitoring.entity.Alert;'
    $content = $content -replace 'import com\.example\.monitoring\.alert\.AlertRule;', 'import com.example.monitoring.entity.AlertRule;'
    $content = $content -replace 'import com\.example\.monitoring\.metric\.Metric;', 'import com.example.monitoring.entity.Metric;'
    $content = $content -replace 'import com\.example\.monitoring\.metric\.MetricDefinition;', 'import com.example.monitoring.entity.MetricDefinition;'
    $content = $content -replace 'import com\.example\.monitoring\.metric\.AgentMetricConfig;', 'import com.example.monitoring.entity.AgentMetricConfig;'
    $content = $content -replace 'import com\.example\.monitoring\.emergency\.EmergencyKnowledge;', 'import com.example.monitoring.entity.EmergencyKnowledge;'
    $content = $content -replace 'import com\.example\.monitoring\.emergency\.EmergencyStep;', 'import com.example.monitoring.entity.EmergencyStep;'
    $content = $content -replace 'import com\.example\.monitoring\.emergency\.AlertRuleEmergency;', 'import com.example.monitoring.entity.AlertRuleEmergency;'
    
    # Repository imports
    $content = $content -replace 'import com\.example\.monitoring\.agent\.AgentRepository;', 'import com.example.monitoring.repository.AgentRepository;'
    $content = $content -replace 'import com\.example\.monitoring\.alert\.AlertRepository;', 'import com.example.monitoring.repository.AlertRepository;'
    $content = $content -replace 'import com\.example\.monitoring\.alert\.AlertRuleRepository;', 'import com.example.monitoring.repository.AlertRuleRepository;'
    $content = $content -replace 'import com\.example\.monitoring\.metric\.MetricRepository;', 'import com.example.monitoring.repository.MetricRepository;'
    $content = $content -replace 'import com\.example\.monitoring\.metric\.MetricDefinitionRepository;', 'import com.example.monitoring.repository.MetricDefinitionRepository;'
    $content = $content -replace 'import com\.example\.monitoring\.metric\.AgentMetricConfigRepository;', 'import com.example.monitoring.repository.AgentMetricConfigRepository;'
    $content = $content -replace 'import com\.example\.monitoring\.emergency\.EmergencyKnowledgeRepository;', 'import com.example.monitoring.repository.EmergencyKnowledgeRepository;'
    $content = $content -replace 'import com\.example\.monitoring\.emergency\.EmergencyStepRepository;', 'import com.example.monitoring.repository.EmergencyStepRepository;'
    $content = $content -replace 'import com\.example\.monitoring\.emergency\.AlertRuleEmergencyRepository;', 'import com.example.monitoring.repository.AlertRuleEmergencyRepository;'
    
    # Service imports
    $content = $content -replace 'import com\.example\.monitoring\.agent\.AgentService;', 'import com.example.monitoring.service.AgentService;'
    $content = $content -replace 'import com\.example\.monitoring\.alert\.AlertService;', 'import com.example.monitoring.service.AlertService;'
    $content = $content -replace 'import com\.example\.monitoring\.metric\.MetricService;', 'import com.example.monitoring.service.MetricService;'
    $content = $content -replace 'import com\.example\.monitoring\.metric\.MetricDefinitionService;', 'import com.example.monitoring.service.MetricDefinitionService;'
    $content = $content -replace 'import com\.example\.monitoring\.metric\.AgentMetricConfigService;', 'import com.example.monitoring.service.AgentMetricConfigService;'
    $content = $content -replace 'import com\.example\.monitoring\.emergency\.EmergencyKnowledgeService;', 'import com.example.monitoring.service.EmergencyKnowledgeService;'
    
    Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
    Write-Host "Updated imports in: $($_.Name)"
}

Write-Host "`nRefactoring completed!"
