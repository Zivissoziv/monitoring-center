@echo off
REM Monitoring Agent Startup Script for Windows

set AGENT_JAR=monitoring-agent.jar
set CONFIG_FILE=agent.properties

echo Starting Monitoring Agent...
start "Monitoring Agent" java -jar %AGENT_JAR% %CONFIG_FILE%
echo Agent started
echo Check the console window for logs