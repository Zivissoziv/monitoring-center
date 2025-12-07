package com.example.agent.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@RestController
public class CommandController {

    @PostMapping("/execute")
    public Map<String, Object> executeCommand(@RequestBody Map<String, String> commandRequest) {
        String command = commandRequest.get("command");
        
        if (command == null || command.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Command is required");
            errorResponse.put("timestamp", System.currentTimeMillis());
            return errorResponse;
        }
        
        System.out.println("[COMMAND] Executing: " + command);
        
        try {
            // Execute the command
            Process process = Runtime.getRuntime().exec(command);
            
            // Read output
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();
            
            // Read stdout
            BufferedReader stdOutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = stdOutReader.readLine()) != null) {
                output.append(line).append("\n");
            }
            stdOutReader.close();
            
            // Read stderr
            BufferedReader stdErrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = stdErrReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
            stdErrReader.close();
            
            // Wait for process completion
            int exitCode = process.waitFor();
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("command", command);
            response.put("exitCode", exitCode);
            response.put("output", output.toString());
            response.put("error", errorOutput.toString());
            response.put("timestamp", System.currentTimeMillis());
            
            System.out.println("[COMMAND] Completed with exit code: " + exitCode);
            return response;
        } catch (Exception e) {
            System.err.println("[COMMAND] Error executing command: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error executing command: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return errorResponse;
        }
    }
}