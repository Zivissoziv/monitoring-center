# Monitoring Center

A simple monitoring system with three components:
1. Frontend (Vue.js)
2. Backend (Spring Boot + MyBatis)
3. Agent (Java)

## Project Structure

```
monitoring-center/
├── backend/          # Spring Boot backend
├── frontend/         # Vue.js frontend
└── agent/            # Java agent
```

## Backend

The backend is a Spring Boot application with:
- Agent management module (CRUD operations for agents)
- Metric collection module (store metrics from agents)
- Alert module (define and check alert rules)

### Running the Backend

```bash
cd backend
mvn spring-boot:run
```

The backend will run on http://localhost:8080

## Frontend

The frontend is a Vue.js application with:
- Agent management UI
- Metric visualization
- Alert rule configuration

### Running the Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend will run on http://localhost:3000

## Agent

The agent is a Java application that:
- Runs an embedded HTTP server
- Responds to metric requests from the backend
- Provides health check endpoint

### Building the Agent

```bash
cd agent
mvn clean package
```

This will create `target/monitoring-agent-1.0-SNAPSHOT.jar` - a standalone executable JAR.

### Deploying the Agent

1. Copy the following files to your target server:
   - `target/monitoring-agent-1.0-SNAPSHOT.jar`
   - `agent.properties.template` (rename to `agent.properties`)
   - `start-agent.sh` (Linux/Mac) or `start-agent.bat` (Windows)
   - `stop-agent.sh` (Linux/Mac) or `stop-agent.bat` (Windows)

2. Edit `agent.properties` to configure:
   - `agent.name`: Unique name for this agent
   - `agent.port`: Port for the agent to listen on (default: 8081)

3. Start the agent:
   - **Linux/Mac**: `./start-agent.sh`
   - **Windows**: `start-agent.bat`
   - **Manual**: `java -jar monitoring-agent-1.0-SNAPSHOT.jar agent.properties`

4. Stop the agent:
   - **Linux/Mac**: `./stop-agent.sh`
   - **Windows**: `stop-agent.bat`

### Configuration

The agent supports configuration via:
1. External `agent.properties` file (recommended for production)
2. Built-in default configuration
3. Command-line argument: `java -jar monitoring-agent-1.0-SNAPSHOT.jar /path/to/config.properties`

### API Endpoints

- GET /health - Health check endpoint
- GET /metrics - Get current CPU and memory metrics

## Features

1. **Agent Management**
   - Add/remove agents
   - View agent status

2. **Metric Collection**
   - Collect CPU and memory usage
   - Store metrics in database

3. **Alerting**
   - Define alert rules based on metrics
   - Check metrics against alert rules

## API Endpoints

### Agents
- GET /api/agents - List all agents
- POST /api/agents - Create a new agent
- PUT /api/agents/{id} - Update an agent
- DELETE /api/agents/{id} - Delete an agent

### Metrics
- GET /api/metrics - List all metrics
- GET /api/metrics/agent/{agentId} - List metrics for an agent
- POST /api/metrics - Save a metric

### Alerts
- GET /api/alerts/rules - List all alert rules
- POST /api/alerts/rules - Create an alert rule
- PUT /api/alerts/rules/{id} - Update an alert rule
- DELETE /api/alerts/rules/{id} - Delete an alert rule