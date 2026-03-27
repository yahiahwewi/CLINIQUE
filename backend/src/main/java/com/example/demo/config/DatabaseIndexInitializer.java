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
}

