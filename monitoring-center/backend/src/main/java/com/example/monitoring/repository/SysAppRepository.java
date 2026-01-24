package com.example.monitoring.repository;

import com.example.monitoring.entity.SysApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysAppRepository extends JpaRepository<SysApp, Long> {
    Optional<SysApp> findByAppCode(String appCode);
    
    boolean existsByAppCode(String appCode);
    
    List<SysApp> findByEnabled(Boolean enabled);
}
