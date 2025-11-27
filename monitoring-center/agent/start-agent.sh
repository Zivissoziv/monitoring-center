#!/bin/bash

# Monitoring Agent Startup Script

AGENT_JAR="monitoring-agent.jar"
CONFIG_FILE="agent.properties"
PID_FILE="agent.pid"

# Check if agent is already running
if [ -f "$PID_FILE" ]; then
    PID=$(cat $PID_FILE)
    if ps -p $PID > /dev/null 2>&1; then
        echo "Agent is already running with PID $PID"
        exit 1
    else
        rm -f $PID_FILE
    fi
fi

# Start the agent
echo "Starting Monitoring Agent..."
nohup java -jar $AGENT_JAR $CONFIG_FILE > agent.log 2>&1 &
echo $! > $PID_FILE
echo "Agent started with PID $(cat $PID_FILE)"
echo "Log file: agent.log"