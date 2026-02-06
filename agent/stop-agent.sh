#!/bin/bash

# Monitoring Agent Stop Script

echo "Stopping Monitoring Agent..."

# Find the PID of the agent process
PID=$(jps -l | grep "monitoring-agent" | awk '{print $1}')

if [ -z "$PID" ]; then
    echo "No running agent process found"
    exit 0
fi

# Kill the process
kill $PID

# Wait for process to stop
sleep 2

# Check if process is still running
if ps -p $PID > /dev/null; then
    echo "Process did not stop gracefully, forcing stop..."
    kill -9 $PID
fi

echo "Agent stopped successfully (PID: $PID)"
