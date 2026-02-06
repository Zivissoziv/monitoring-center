package com.example.monitoring.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "sys_role_menus")
public class SysRoleMenu {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "role_id", nullable = false)
    private Long roleId;
    
    @Column(name = "menu_id", nullable = false)
    private Long menuId;
}
