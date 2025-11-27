#!/bin/bash

# Monitoring Agent Stop Script

PID_FILE="agent.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "PID file not found. Agent may not be running."
    exit 1
fi

PID=$(cat $PID_FILE)

if ps -p $PID > /dev/null 2>&1; then
    echo "Stopping agent with PID $PID..."
    kill $PID
    
    # Wait for process to stop
    for i in {1..10}; do
        if ! ps -p $PID > /dev/null 2>&1; then
            echo "Agent stopped successfully"
            rm -f $PID_FILE
            exit 0
        fi
        sleep 1
    done
    
    # Force kill if still running
    echo "Force stopping agent..."
    kill -9 $PID
    rm -f $PID_FILE
    echo "Agent force stopped"
else
    echo "Agent is not running"
    rm -f $PID_FILE
fi