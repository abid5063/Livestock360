package com.livestock360.springbackend.config;

import com.livestock360.springbackend.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    @Autowired
    private AdminService adminService;

    @Override
    public void run(String... args) throws Exception {
        // Create default admin account if it doesn't exist
        adminService.createDefaultAdminIfNotExists();
    }
}