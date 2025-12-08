package com.example.monitoring.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "agents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Agent {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;
    
    private String name;
    private String ip;
    private int port;
    private String status; // ACTIVE, INACTIVE, DISCONNECTED
    
    public Agent(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.status = "INACTIVE";
    }
}