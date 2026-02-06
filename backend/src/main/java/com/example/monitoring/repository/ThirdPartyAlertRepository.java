package com.example.monitoring.repository;

import com.example.monitoring.entity.ThirdPartyAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThirdPartyAlertRepository extends JpaRepository<ThirdPartyAlert, Long> {
    
    List<ThirdPartyAlert> findByChannelCode(String channelCode);
    
    List<ThirdPartyAlert> findByExternalAlertIdOrderByReceivedTimeDesc(String externalAlertId);
    
    @Query("SELECT COUNT(t) FROM ThirdPartyAlert t WHERE t.channelCode = :channelCode")
    long countByChannelCode(@Param("channelCode") String channelCode);
    
    @Query("SELECT COUNT(t) FROM ThirdPartyAlert t WHERE t.channelCode = :channelCode AND t.pushStatus = 'SUCCESS'")
    long countSuccessByChannelCode(@Param("channelCode") String channelCode);
    
    @Query("SELECT COUNT(t) FROM ThirdPartyAlert t WHERE t.channelCode = :channelCode AND t.pushStatus = 'FAILED'")
    long countFailedByChannelCode(@Param("channelCode") String channelCode);
    
    @Query("SELECT t.channelCode, COUNT(t) as total, " +
           "SUM(CASE WHEN t.pushStatus = 'SUCCESS' THEN 1 ELSE 0 END) as success, " +
           "SUM(CASE WHEN t.pushStatus = 'FAILED' THEN 1 ELSE 0 END) as failed " +
           "FROM ThirdPartyAlert t GROUP BY t.channelCode")
    List<Object[]> getStatisticsByChannel();
}
