package com.example.monitoring.entity;

import com.example.monitoring.enums.AgentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "agents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Agent {
    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;  // Manually set, should match agent.name from agent config
    
    private String name;
    private String ip;
    private int port;
    
    @Column(name = "app_code", length = 10)
    private String appCode;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AgentStatus status;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public Agent(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.appCode = null;
        this.status = AgentStatus.INACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Agent(String name, String ip, int port, String appCode) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.appCode = appCode;
        this.status = AgentStatus.INACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}