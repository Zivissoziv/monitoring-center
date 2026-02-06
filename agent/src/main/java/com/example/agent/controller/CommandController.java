package com.example.agent.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Slf4j
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
        
        log.info("Executing command: {}", command);
        log.debug("OS: {}", System.getProperty("os.name"));
        
        try {
            // Detect OS and use appropriate shell
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder processBuilder;
            
            if (os.contains("win")) {
                // Windows: use cmd.exe /c
                processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
                log.debug("Using Windows shell");
            } else {
                // Linux/Unix: use /bin/bash -c (bash is more feature-rich than sh)
                processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
                log.debug("Using bash shell");
            }
            
            // Redirect error stream if needed for debugging
            processBuilder.redirectErrorStream(false);
            
            // Execute the command
            Process process = processBuilder.start();
            
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
            
            // Trim outputs
            String finalOutput = output.toString().trim();
            String finalError = errorOutput.toString().trim();
            
            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("command", command);
            response.put("exitCode", exitCode);
            response.put("output", finalOutput);
            response.put("error", finalError);
            response.put("timestamp", System.currentTimeMillis());
            
            if (exitCode == 0) {
                log.info("Command completed successfully, output length: {}", finalOutput.length());
                log.debug("Output: {}", finalOutput);
            } else {
                log.error("Command failed with exit code: {}", exitCode);
                log.error("Stdout: {}", finalOutput);
                log.error("Stderr: {}", finalError);
            }
            return response;
        } catch (Exception e) {
            log.error("Error executing command: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error executing command: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return errorResponse;
        }
    }
}