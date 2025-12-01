@echo off
REM Monitoring Agent Startup Script for Windows

set AGENT_JAR=monitoring-agent-1.0-SNAPSHOT.jar
set CONFIG_FILE=agent.properties

echo Starting Monitoring Agent...
start "Monitoring Agent" java -jar %AGENT_JAR% %CONFIG_FILE%
echo Agent started
echo Check the console window for logs