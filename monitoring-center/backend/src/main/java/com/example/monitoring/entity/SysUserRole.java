package com.example.monitoring.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "sys_user_roles")
public class SysUserRole {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "role_id", nullable = false)
    private Long roleId;
}
