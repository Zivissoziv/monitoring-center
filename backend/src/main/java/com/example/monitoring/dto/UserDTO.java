package com.example.monitoring.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class UserDTO {
    
    private Long id;
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    private String username;
    
    @Size(min = 6, max = 100, message = "密码长度必须在6-100之间")
    private String password;
    
    @Size(max = 100, message = "昵称长度不能超过100")
    private String nickname;
    
    private Boolean enabled;
    
    private List<Long> roleIds;
}
