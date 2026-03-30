package com.example.demo.service;

import com.example.demo.entity.User;

import java.time.LocalDateTime;

public interface PasswordResetNotificationService {
    void sendPasswordResetEmail(User user, String token, LocalDateTime expiresAt);
}
