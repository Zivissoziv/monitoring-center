#!/bin/bash

# Monitoring Agent Startup Script

AGENT_JAR="monitoring-agent-1.0-SNAPSHOT.jar"
CONFIG_FILE="agent.properties"

# Check if config file exists, if not copy from template
if [ ! -f "$CONFIG_FILE" ]; then
    if [ -f "agent.properties.template" ]; then
        echo "Creating $CONFIG_FILE from template..."
        cp agent.properties.template $CONFIG_FILE
    else
        echo "Warning: Neither $CONFIG_FILE nor agent.properties.template found"
        echo "Agent will use default configuration"
    fi
fi

# Start the agent with proper logging
echo "Starting Monitoring Agent..."
if [ -f "$CONFIG_FILE" ]; then
    nohup java -jar $AGENT_JAR --spring.config.location=file:$CONFIG_FILE > agent.log 2>&1 &
else
    nohup java -jar $AGENT_JAR > agent.log 2>&1 &
fi
echo "Agent started in background"
echo "Log file: agent.log"