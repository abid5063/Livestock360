package com.livestock360.springbackend.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cloudinary configuration class - replicating Node.js cloudinary.js functionality
 * Configures Cloudinary with the exact same credentials as the Node.js backend
 */
@Configuration
public class CloudinaryConfig {
    
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "dcvkttktl",
            "api_key", "931199518597945", 
            "api_secret", "efPviLx3FH1b_NsMxPamZKS3_jo",
            "secure", true
        ));
    }
}