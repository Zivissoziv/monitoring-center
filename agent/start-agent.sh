#!/bin/bash

# Monitoring Agent Startup Script

AGENT_JAR="monitoring-agent-1.0-SNAPSHOT.jar"

# Start the agent with proper logging
echo "Starting Monitoring Agent..."
nohup java -jar $AGENT_JAR > agent.log 2>&1 &
echo "Agent started in background"
echo "Log file: agent.log"