package com.example.monitoring.agent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AgentService {
    
    @Autowired
    private AgentRepository agentRepository;
    
    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }
    
    public Optional<Agent> getAgentById(Long id) {
        return agentRepository.findById(id);
    }
    
    public Agent createAgent(Agent agent) {
        return agentRepository.save(agent);
    }
    
    public Agent updateAgent(Long id, Agent agentDetails) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent not found with id: " + id));
        
        agent.setName(agentDetails.getName());
        agent.setIp(agentDetails.getIp());
        agent.setPort(agentDetails.getPort());
        agent.setStatus(agentDetails.getStatus());
        
        return agentRepository.save(agent);
    }
    
    public void deleteAgent(Long id) {
        Agent agent = agentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agent not found with id: " + id));
        
        agentRepository.delete(agent);
    }
}