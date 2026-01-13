package com.example.monitoring.repository;

import com.example.monitoring.entity.AlertChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlertChannelRepository extends JpaRepository<AlertChannel, Long> {
    Optional<AlertChannel> findByChannelCode(String channelCode);
    
    boolean existsByChannelCode(String channelCode);
}
