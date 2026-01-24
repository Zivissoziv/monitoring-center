package com.example.monitoring.repository;

import com.example.monitoring.entity.Agent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentRepository extends JpaRepository<Agent, String> {
    List<Agent> findByAppCode(String appCode);
    
    List<Agent> findByAppCodeIn(List<String> appCodes);
}