package com.example.monitoring.repository;

import com.example.monitoring.entity.SysUserApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SysUserAppRepository extends JpaRepository<SysUserApp, Long> {
    List<SysUserApp> findByUserId(Long userId);
    
    List<SysUserApp> findByAppCode(String appCode);
    
    @Query("SELECT ua.appCode FROM SysUserApp ua WHERE ua.userId = :userId")
    List<String> findAppCodesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT ua.userId FROM SysUserApp ua WHERE ua.appCode = :appCode")
    List<Long> findUserIdsByAppCode(@Param("appCode") String appCode);
    
    void deleteByUserId(Long userId);
    
    void deleteByAppCode(String appCode);
    
    boolean existsByUserIdAndAppCode(Long userId, String appCode);
}
