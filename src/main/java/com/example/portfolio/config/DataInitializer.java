package com.example.portfolio.config;

import com.example.portfolio.entity.AdminUser;
import com.example.portfolio.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首次启动时自动创建管理员账号
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private final AdminUserRepository adminUserRepository;
    private final BCryptPasswordEncoder encoder;

    @Value("${app.admin.username}")
    private String username;

    @Value("${app.admin.password}")
    private String password;

    public DataInitializer(AdminUserRepository adminUserRepository, BCryptPasswordEncoder encoder) {
        this.adminUserRepository = adminUserRepository;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminUserRepository.count() > 0) {
            return;
        }
        AdminUser admin = new AdminUser();
        admin.setUsername(username);
        admin.setPasswordHash(encoder.encode(password));
        adminUserRepository.save(admin);
    }
}
