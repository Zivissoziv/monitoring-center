@echo off
REM Monitoring Agent Startup Script for Windows

set AGENT_JAR=monitoring-agent-1.0-SNAPSHOT.jar
set CONFIG_FILE=agent.properties

REM Check if config file exists, if not copy from template
if not exist "%CONFIG_FILE%" (
    if exist "agent.properties.template" (
        echo Creating %CONFIG_FILE% from template...
        copy agent.properties.template %CONFIG_FILE%
    ) else (
        echo Warning: Neither %CONFIG_FILE% nor agent.properties.template found
        echo Agent will use default configuration
    )
)

REM Start the agent with proper logging
echo Starting Monitoring Agent...
if exist "%CONFIG_FILE%" (
    start "Monitoring Agent" java -jar %AGENT_JAR% --spring.config.location=file:%CONFIG_FILE% > agent.log 2>&1
) else (
    start "Monitoring Agent" java -jar %AGENT_JAR% > agent.log 2>&1
)
echo Agent started
echo Log file: agent.log