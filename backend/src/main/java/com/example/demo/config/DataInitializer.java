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
import java.util.Map;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            Map<String, String> roles = Map.of(
                    "ROLE_USER", "Patient role",
                    "ROLE_DOCTOR", "Doctor role",
                    "ROLE_NURSE", "Nurse role",
                    "ROLE_ADMIN", "Administrator role"
            );

            roles.forEach((name, description) -> {
                if (roleRepository.findByName(name).isEmpty()) {
                    roleRepository.save(Role.builder()
                            .name(name)
                            .description(description)
                            .build());
                }
            });

            createUserIfMissing(userRepository, roleRepository, passwordEncoder,
                    "admin@example.com", "Admin", "User", "admin123", "ROLE_ADMIN");
            createUserIfMissing(userRepository, roleRepository, passwordEncoder,
                    "doctor@example.com", "Emily", "Stone", "password123", "ROLE_DOCTOR");
            createUserIfMissing(userRepository, roleRepository, passwordEncoder,
                    "nurse@example.com", "Nina", "Brooks", "password123", "ROLE_NURSE");
            createUserIfMissing(userRepository, roleRepository, passwordEncoder,
                    "john@example.com", "John", "Doe", "password123", "ROLE_USER");
        };
    }

    private void createUserIfMissing(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            String email,
            String firstName,
            String lastName,
            String password,
            String roleName
    ) {
        if (!userRepository.existsByEmail(email)) {
            Role role = roleRepository.findByName(roleName).orElseThrow();
            User user = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .firstName(firstName)
                    .lastName(lastName)
                    .roles(new HashSet<>(Collections.singletonList(role)))
                    .enabled(true)
                    .build();
            userRepository.save(user);
        }
    }
}
