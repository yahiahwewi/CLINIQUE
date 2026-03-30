package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final PasswordResetNotificationService passwordResetNotificationService;
    private final long passwordResetExpirationMs;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                      PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager,
                      JwtUtil jwtUtil, UserDetailsService userDetailsService,
                      PasswordResetNotificationService passwordResetNotificationService,
                      @Value("${app.password-reset.expiration-ms:3600000}") long passwordResetExpirationMs) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.passwordResetNotificationService = passwordResetNotificationService;
        this.passwordResetExpirationMs = passwordResetExpirationMs;
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        String normalizedEmail = normalizeEmail(registerRequest.getEmail());
        String normalizedFirstName = normalizeText(registerRequest.getFirstName());
        String normalizedLastName = normalizeText(registerRequest.getLastName());
        String password = registerRequest.getPassword();
        String confirmPassword = registerRequest.getConfirmPassword();

        // Validate passwords match
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Check if email already exists
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email already in use");
        }

        // Create new user
        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(password))
                .firstName(normalizedFirstName)
                .lastName(normalizedLastName)
                .enabled(true)
                .build();

        // Assign default USER role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default ROLE_USER role not found"));
        user.addRole(userRole);

        User savedUser = userRepository.save(user);

        // Generate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        return buildAuthResponse(token, savedUser);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        String normalizedEmail = normalizeEmail(loginRequest.getEmail());

        // Authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        loginRequest.getPassword()
                )
        );

        // Get user details
        UserDetails userDetails = userDetailsService.loadUserByUsername(normalizedEmail);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate token
        String token = jwtUtil.generateToken(userDetails);

        return buildAuthResponse(token, user);
    }

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusNanos(passwordResetExpirationMs * 1_000_000);
            user.setResetPasswordToken(token);
            user.setResetPasswordTokenExpiresAt(expiresAt);
            userRepository.save(user);
            passwordResetNotificationService.sendPasswordResetEmail(user, token, expiresAt);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Reset token is invalid or expired"));

        if (user.getResetPasswordTokenExpiresAt() == null || user.getResetPasswordTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset token is invalid or expired");
        }

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiresAt(null);
        userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        return normalizeText(email).toLowerCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toList()))
                .build();
    }
}
