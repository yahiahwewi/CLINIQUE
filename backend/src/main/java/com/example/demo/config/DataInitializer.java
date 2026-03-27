package com.example.demo.config;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Create default roles if they don't exist
            if (roleRepository.findByName("ROLE_USER").isEmpty()) {
                Role userRole = Role.builder()
                        .name("ROLE_USER")
                        .description("Default user role")
                        .build();
                roleRepository.save(userRole);
            }

            if (roleRepository.findByName("ROLE_ADMIN").isEmpty()) {
                Role adminRole = Role.builder()
                        .name("ROLE_ADMIN")
                        .description("Administrator role")
                        .build();
                roleRepository.save(adminRole);
            }

            // Create admin user if not exists
            if (!userRepository.existsByEmail("admin@example.com")) {
                Role adminRole = roleRepository.findByName("ROLE_ADMIN").get();
                User admin = User.builder()
                        .email("admin@example.com")
                        .password(passwordEncoder.encode("admin123"))
                        .firstName("Admin")
                        .lastName("User")
                        .roles(new HashSet<>(Collections.singletonList(adminRole)))
                        .enabled(true)
                        .build();
                userRepository.save(admin);
            }
        };
    }
}
