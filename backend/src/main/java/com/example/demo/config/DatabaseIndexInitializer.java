package com.example.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.logging.Logger;

@Configuration
public class DatabaseIndexInitializer {
    private static final Logger logger = Logger.getLogger(DatabaseIndexInitializer.class.getName());

    @Bean
    public CommandLineRunner initializeIndexes(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement()) {
                
                // Index on users.email for login and lookups
                executeIfNotExists(statement, 
                    "CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)",
                    "idx_users_email");
                
                // Index on users.enabled for filtering active/inactive users
                executeIfNotExists(statement,
                    "CREATE INDEX IF NOT EXISTS idx_users_enabled ON users(enabled)",
                    "idx_users_enabled");
                
                // Index on user_roles for role lookups
                executeIfNotExists(statement,
                    "CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id)",
                    "idx_user_roles_user_id");
                
                // Index on roles.name for role lookups
                executeIfNotExists(statement,
                    "CREATE INDEX IF NOT EXISTS idx_roles_name ON roles(name)",
                    "idx_roles_name");
                
                // Insert demo users if not exist
                insertDemoUsers(statement);
                
                logger.info("Database indexes verified/created successfully");
                
            } catch (Exception e) {
                logger.warning("Error initializing database indexes: " + e.getMessage());
            }
        };
    }

    private void executeIfNotExists(Statement statement, String sql, String indexName) {
        try {
            statement.execute(sql);
        } catch (Exception e) {
            if (!e.getMessage().contains("already exists")) {
                throw new RuntimeException("Failed to create index " + indexName, e);
            }
        }
    }

    private void insertDemoUsers(Statement statement) {
        try {
            // Insert roles if not exist with ROLE_ prefix for Spring Security (portable syntax for H2/PostgreSQL)
            statement.execute("INSERT INTO roles (name) SELECT 'ROLE_PATIENT' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_PATIENT')");
            statement.execute("INSERT INTO roles (name) SELECT 'ROLE_DOCTOR' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_DOCTOR')");
            statement.execute("INSERT INTO roles (name) SELECT 'ROLE_NURSE' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_NURSE')");
            statement.execute("INSERT INTO roles (name) SELECT 'ROLE_ADMIN' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_ADMIN')");
            statement.execute("INSERT INTO roles (name) SELECT 'ROLE_USER' WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_USER')");

            // Insert demo users - BCrypt hash for "password123"
            // Hash from documentation: $2a$10$K6eoHi8qW7v8Nuz9JcJ2.OqTq6r1KYhZKHDT0/1NwT2p/yDHWZsNm
            String passwordHash = "$2a$10$K6eoHi8qW7v8Nuz9JcJ2.OqTq6r1KYhZKHDT0/1NwT2p/yDHWZsNm";
            
            statement.execute("INSERT INTO users (email, password, first_name, last_name, enabled) " +
                "SELECT 'john@example.com', '" + passwordHash + "', 'John', 'Doe', true " +
                "WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'john@example.com')");
            statement.execute("INSERT INTO users (email, password, first_name, last_name, enabled) " +
                "SELECT 'doctor@example.com', '" + passwordHash + "', 'Dr.', 'Smith', true " +
                "WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'doctor@example.com')");
            statement.execute("INSERT INTO users (email, password, first_name, last_name, enabled) " +
                "SELECT 'nurse@example.com', '" + passwordHash + "', 'Jane', 'Nurse', true " +
                "WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'nurse@example.com')");
            statement.execute("INSERT INTO users (email, password, first_name, last_name, enabled) " +
                "SELECT 'admin@example.com', '" + passwordHash + "', 'Admin', 'User', true " +
                "WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@example.com')");

            // Assign roles with ROLE_ prefix
            statement.execute("INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r " +
                "WHERE u.email = 'john@example.com' AND r.name = 'ROLE_USER' " +
                "AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id)");
            statement.execute("INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r " +
                "WHERE u.email = 'doctor@example.com' AND r.name = 'ROLE_DOCTOR' " +
                "AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id)");
            statement.execute("INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r " +
                "WHERE u.email = 'nurse@example.com' AND r.name = 'ROLE_NURSE' " +
                "AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id)");
            statement.execute("INSERT INTO user_roles (user_id, role_id) SELECT u.id, r.id FROM users u, roles r " +
                "WHERE u.email = 'admin@example.com' AND r.name = 'ROLE_ADMIN' " +
                "AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = r.id)");

            logger.info("Demo users initialized successfully");
        } catch (Exception e) {
            logger.warning("Error inserting demo users: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

