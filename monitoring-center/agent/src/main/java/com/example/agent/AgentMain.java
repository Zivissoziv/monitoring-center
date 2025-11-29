package com.example.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class AgentMain {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Agent configuration
    private static String agentName;
    private static int agentPort;
    
    public static void main(String[] args) {
        System.out.println("Starting Monitoring Agent...");
        
        // Load configuration
        loadConfiguration(args);
        
        System.out.println("Agent Name: " + agentName);
        System.out.println("Agent Port: " + agentPort);
        
        // Start HTTP server to expose metrics API
        try {
            startHttpServer();
        } catch (IOException e) {
            System.err.println("Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
        System.out.println("Agent is running on port " + agentPort + ". Press Ctrl+C to stop.");
    }
    
    private static void loadConfiguration(String[] args) {
        Properties properties = new Properties();
        
        // Try to load from external config file first
        String configFile = "agent.properties";
        if (args.length > 0) {
            configFile = args[0];
        }
        
        File externalConfig = new File(configFile);
        if (externalConfig.exists()) {
            System.out.println("Loading configuration from: " + externalConfig.getAbsolutePath());
            try (FileInputStream fis = new FileInputStream(externalConfig)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Failed to load external config file: " + e.getMessage());
            }
        } else {
            // Load from classpath
            System.out.println("Loading default configuration from classpath");
            try (InputStream is = AgentMain.class.getClassLoader().getResourceAsStream("agent.properties")) {
                if (is != null) {
                    properties.load(is);
                } else {
                    System.err.println("No configuration file found, using defaults");
                }
            } catch (IOException e) {
                System.err.println("Failed to load classpath config: " + e.getMessage());
            }
        }
        
        // Load configuration with defaults
        agentName = properties.getProperty("agent.name", "MonitoringAgent-" + getHostname());
        agentPort = Integer.parseInt(properties.getProperty("agent.port", "8081"));
    }
    
    private static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static void startHttpServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(agentPort), 0);
        
        // Health check endpoint
        server.createContext("/health", exchange -> {
            String response = "{\"status\":\"UP\",\"name\":\"" + agentName + "\"}";
            sendJsonResponse(exchange, 200, response);
        });
        
        // Metrics endpoint
        server.createContext("/metrics", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    Map<String, Object> metrics = collectMetrics();
                    String json = objectMapper.writeValueAsString(metrics);
                    sendJsonResponse(exchange, 200, json);
                } catch (Exception e) {
                    String error = "{\"error\":\"" + e.getMessage() + "\"}";
                    sendJsonResponse(exchange, 500, error);
                }
            } else {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        });
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("HTTP server started on port " + agentPort);
    }
    
    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
    
    private static Map<String, Object> collectMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Collect CPU and Memory metrics
        double cpuUsage = getCpuUsage();
        double memoryUsage = getMemoryUsage();
        
        metrics.put("cpu", cpuUsage);
        metrics.put("memory", memoryUsage);
        metrics.put("timestamp", System.currentTimeMillis());
        metrics.put("agentName", agentName);
        
        return metrics;
    }
    
    private static double getCpuUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        return osBean.getSystemLoadAverage() * 100; // This is a simplified approach
    }
    
    private static double getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return (double) usedMemory / totalMemory * 100;
    }
    

}