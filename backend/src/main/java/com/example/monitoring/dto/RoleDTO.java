package com.example.monitoring.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class RoleDTO {
    
    private Long id;
    
    @NotBlank(message = "角色编码不能为空")
    @Size(min = 2, max = 50, message = "角色编码长度必须在2-50之间")
    private String roleCode;
    
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 100, message = "角色名称长度不能超过100")
    private String roleName;
    
    @Size(max = 500, message = "描述长度不能超过500")
    private String description;
    
    private List<Long> menuIds;
}
