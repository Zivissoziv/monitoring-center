package com.example.monitoring.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "sys_menus")
public class SysMenu {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "menu_code", nullable = false, unique = true, length = 50)
    private String menuCode;
    
    @Column(name = "menu_name", nullable = false, length = 100)
    private String menuName;
    
    @Column(length = 50)
    private String icon;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @Column(nullable = false)
    private Boolean enabled = true;
}
