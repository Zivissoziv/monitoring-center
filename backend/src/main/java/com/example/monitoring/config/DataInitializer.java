package com.example.monitoring.config;

import com.example.monitoring.entity.SysUser;
import com.example.monitoring.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) {
        // Update admin password with proper BCrypt encoding
        userRepository.findByUsername("admin").ifPresent(user -> {
            if (!user.getPassword().startsWith("$2a$")) {
                user.setPassword(passwordEncoder.encode("admin123"));
                userRepository.save(user);
                log.info("Admin password has been properly encoded");
            }
        });
        
        // Update demo password with proper BCrypt encoding
        userRepository.findByUsername("demo").ifPresent(user -> {
            if (!user.getPassword().startsWith("$2a$")) {
                user.setPassword(passwordEncoder.encode("demo123"));
                userRepository.save(user);
                log.info("Demo user password has been properly encoded");
            }
        });
    }
}
