package com.example.monitoring.repository;

import com.example.monitoring.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    
    Optional<SysUser> findByUsername(String username);
    
    boolean existsByUsername(String username);
    
    @Query("SELECT u FROM SysUser u WHERE u.enabled = true")
    List<SysUser> findAllEnabled();
}
