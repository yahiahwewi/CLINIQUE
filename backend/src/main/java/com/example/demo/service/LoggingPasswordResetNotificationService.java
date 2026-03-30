package com.example.demo.service;

import com.example.demo.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LoggingPasswordResetNotificationService implements PasswordResetNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingPasswordResetNotificationService.class);

    @Override
    public void sendPasswordResetEmail(User user, String token, LocalDateTime expiresAt) {
        LOGGER.info(
                "Mock password reset email for {}: use token {} before {}",
                user.getEmail(),
                token,
                expiresAt
        );
    }
}
