package com.example.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AgentMain {
    private static String BACKEND_URL;
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    // Agent configuration
    private static long agentId = 0;
    private static String agentName;
    private static int agentPort;
    private static int metricInterval;
    
    public static void main(String[] args) {
        System.out.println("Starting Monitoring Agent...");
        
        // Load configuration
        loadConfiguration(args);
        
        System.out.println("Agent Name: " + agentName);
        System.out.println("Backend URL: " + BACKEND_URL);
        System.out.println("Metric Collection Interval: " + metricInterval + " seconds");
        
        // Register the agent with the backend
        registerAgent();
        
        // Schedule metric collection
        scheduler.scheduleAtFixedRate(AgentMain::collectAndSendMetrics, 0, metricInterval, TimeUnit.SECONDS);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down agent...");
            scheduler.shutdown();
        }));
        
        // Keep the agent running
        System.out.println("Agent is running. Press Ctrl+C to stop.");
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("Agent interrupted.");
        }
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
        BACKEND_URL = properties.getProperty("backend.url", "http://localhost:8080");
        metricInterval = Integer.parseInt(properties.getProperty("metric.interval.seconds", "10"));
    }
    
    private static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private static void registerAgent() {
        try {
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            
            Map<String, Object> agentData = new HashMap<>();
            agentData.put("name", agentName);
            agentData.put("ip", ipAddress);
            agentData.put("port", agentPort);
            
            String json = objectMapper.writeValueAsString(agentData);
            
            RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(BACKEND_URL + "/api/agents")
                    .post(body)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    System.out.println("Agent registered successfully: " + responseBody);
                    // Extract agent ID from response
                    Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                    if (responseMap.containsKey("id")) {
                        agentId = ((Number) responseMap.get("id")).longValue();
                        System.out.println("Agent ID: " + agentId);
                    }
                } else {
                    System.err.println("Failed to register agent: " + response.code());
                }
            }
        } catch (Exception e) {
            System.err.println("Error registering agent: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void collectAndSendMetrics() {
        try {
            // Collect CPU and Memory metrics
            double cpuUsage = getCpuUsage();
            double memoryUsage = getMemoryUsage();
            
            System.out.println("Collected metrics - CPU: " + cpuUsage + "%, Memory: " + memoryUsage + "%");
            
            // Send metrics to backend
            sendMetric("CPU", cpuUsage);
            sendMetric("MEMORY", memoryUsage);
        } catch (Exception e) {
            System.err.println("Error collecting or sending metrics: " + e.getMessage());
            e.printStackTrace();
        }
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
    
    private static void sendMetric(String metricType, double value) {
        try {
            Map<String, Object> metricData = new HashMap<>();
            metricData.put("agentId", agentId);
            metricData.put("metricType", metricType);
            metricData.put("value", value);
            
            String json = objectMapper.writeValueAsString(metricData);
            
            RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
            Request request = new Request.Builder()
                    .url(BACKEND_URL + "/api/metrics")
                    .post(body)
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    System.out.println("Metric sent successfully: " + metricType + " = " + value);
                } else {
                    System.err.println("Failed to send metric: " + response.code());
                }
            }
        } catch (Exception e) {
            System.err.println("Error sending metric: " + e.getMessage());
            e.printStackTrace();
        }
    }
}