package com.example.monitoring.controller;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for managing Quartz scheduled jobs
 */
@Slf4j
@RestController
@RequestMapping("/api/scheduler")
@CrossOrigin(origins = "*")
public class SchedulerController {
    
    @Autowired
    private Scheduler scheduler;
    
    /**
     * Get all jobs with their status
     */
    @GetMapping("/jobs")
    public ResponseEntity<List<JobInfo>> getAllJobs() {
        try {
            List<JobInfo> jobs = new ArrayList<>();
            
            for (String groupName : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                    
                    JobInfo info = new JobInfo();
                    info.setJobName(jobKey.getName());
                    info.setJobGroup(jobKey.getGroup());
                    
                    if (!triggers.isEmpty()) {
                        Trigger trigger = triggers.get(0);
                        info.setTriggerName(trigger.getKey().getName());
                        info.setTriggerState(scheduler.getTriggerState(trigger.getKey()).name());
                        
                        if (trigger.getNextFireTime() != null) {
                            info.setNextFireTime(trigger.getNextFireTime().getTime());
                        }
                        if (trigger.getPreviousFireTime() != null) {
                            info.setPreviousFireTime(trigger.getPreviousFireTime().getTime());
                        }
                    }
                    
                    jobs.add(info);
                }
            }
            
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            log.error("Error getting jobs: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Pause a job
     */
    @PostMapping("/jobs/{jobName}/pause")
    public ResponseEntity<Map<String, String>> pauseJob(@PathVariable String jobName) {
        try {
            JobKey jobKey = new JobKey(jobName);
            scheduler.pauseJob(jobKey);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Job paused: " + jobName);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error pausing job: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Resume a job
     */
    @PostMapping("/jobs/{jobName}/resume")
    public ResponseEntity<Map<String, String>> resumeJob(@PathVariable String jobName) {
        try {
            JobKey jobKey = new JobKey(jobName);
            scheduler.resumeJob(jobKey);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Job resumed: " + jobName);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error resuming job: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Trigger a job immediately
     */
    @PostMapping("/jobs/{jobName}/trigger")
    public ResponseEntity<Map<String, String>> triggerJob(@PathVariable String jobName) {
        try {
            JobKey jobKey = new JobKey(jobName);
            scheduler.triggerJob(jobKey);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Job triggered: " + jobName);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error triggering job: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Update job schedule interval
     */
    @PutMapping("/jobs/{jobName}/interval")
    public ResponseEntity<Map<String, String>> updateJobInterval(
            @PathVariable String jobName,
            @RequestBody IntervalRequest request) {
        try {
            TriggerKey triggerKey = new TriggerKey(jobName + "Trigger");
            
            // Build new trigger with updated interval
            Trigger newTrigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(request.getIntervalSeconds())
                            .repeatForever())
                    .build();
            
            // Reschedule job
            scheduler.rescheduleJob(triggerKey, newTrigger);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Job interval updated to " + request.getIntervalSeconds() + " seconds");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating job interval: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @Data
    public static class JobInfo {
        private String jobName;
        private String jobGroup;
        private String triggerName;
        private String triggerState;
        private Long nextFireTime;
        private Long previousFireTime;
    }
    
    @Data
    public static class IntervalRequest {
        private int intervalSeconds;
    }
}
