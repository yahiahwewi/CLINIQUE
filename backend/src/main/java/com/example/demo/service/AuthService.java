package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.ApprovalStatus;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Set<String> SELF_SIGNUP_ROLES = Set.of("USER", "DOCTOR", "NURSE");

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

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        String normalizedEmail = normalizeEmail(registerRequest.getEmail());
        String normalizedFirstName = normalizeText(registerRequest.getFirstName());
        String normalizedLastName = normalizeText(registerRequest.getLastName());
        String password = registerRequest.getPassword();
        String confirmPassword = registerRequest.getConfirmPassword();

        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email already in use");
        }

        String requestedRole = normalizeRequestedRole(registerRequest.getRequestedRole());
        boolean isPatient = "USER".equals(requestedRole);

        // Patients auto-approve and can sign in immediately. Clinical staff
        // (DOCTOR / NURSE) start PENDING and must be approved by an admin.
        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(password))
                .firstName(normalizedFirstName)
                .lastName(normalizedLastName)
                .enabled(isPatient)
                .approvalStatus(isPatient ? ApprovalStatus.APPROVED : ApprovalStatus.PENDING)
                .requestedRole("ROLE_" + requestedRole)
                .roles(new HashSet<>())
                .build();

        // Always grant ROLE_USER so the principal has at least one role. For
        // patients that's the final role; for staff the requested clinical
        // role is added on approval.
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Default ROLE_USER role not found"));
        user.addRole(userRole);

        User savedUser = userRepository.save(user);

        if (isPatient) {
            return AuthResponse.builder()
                    .token("")
                    .id(savedUser.getId())
                    .email(savedUser.getEmail())
                    .firstName(savedUser.getFirstName())
                    .lastName(savedUser.getLastName())
                    .roles(List.of("ROLE_USER"))
                    .approvalStatus(ApprovalStatus.APPROVED.name())
                    .message("Welcome, " + savedUser.getFirstName() + " — your account is ready. Sign in to book your first appointment.")
                    .build();
        }

        return AuthResponse.builder()
                .token("")
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .roles(List.of())
                .approvalStatus(ApprovalStatus.PENDING.name())
                .message("Thanks " + savedUser.getFirstName() + " — your " + requestedRole.toLowerCase() + " account is pending administrator approval. You'll be able to sign in once it's approved.")
                .build();
    }

    public AuthResponse login(LoginRequest loginRequest) {
        String normalizedEmail = normalizeEmail(loginRequest.getEmail());

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        ApprovalStatus status = user.getApprovalStatus();
        if (status == ApprovalStatus.PENDING) {
            throw new AccessDeniedException("Your account is pending administrator approval.");
        }
        if (status == ApprovalStatus.REJECTED) {
            throw new AccessDeniedException("Your registration was not approved. Please contact an administrator.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        normalizedEmail,
                        loginRequest.getPassword()
                )
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(normalizedEmail);
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

    private String normalizeRequestedRole(String requested) {
        if (requested == null || requested.isBlank()) {
            return "USER";
        }
        String upper = requested.trim().toUpperCase(Locale.ROOT);
        if (upper.startsWith("ROLE_")) upper = upper.substring(5);
        if ("PATIENT".equals(upper)) upper = "USER";
        if (!SELF_SIGNUP_ROLES.contains(upper)) {
            throw new IllegalArgumentException(
                    "Requested role must be one of: PATIENT, DOCTOR, NURSE.");
        }
        return upper;
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
                .approvalStatus(
                        user.getApprovalStatus() == null
                                ? ApprovalStatus.APPROVED.name()
                                : user.getApprovalStatus().name()
                )
                .build();
    }
}
