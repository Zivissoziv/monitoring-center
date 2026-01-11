package com.example.monitoring.repository;

import com.example.monitoring.entity.EmergencyKnowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmergencyKnowledgeRepository extends JpaRepository<EmergencyKnowledge, Long> {
}
