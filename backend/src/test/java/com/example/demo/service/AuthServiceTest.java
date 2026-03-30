package com.example.demo.service;

import com.example.demo.dto.AuthResponse;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private PasswordResetNotificationService passwordResetNotificationService;

    private AuthService authService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(
                userRepository,
                roleRepository,
                passwordEncoder,
                authenticationManager,
                jwtUtil,
                userDetailsService,
                passwordResetNotificationService,
                3_600_000L
        );
    }

    @Test
    void testRegisterSuccess() {
        // Arrange
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .confirmPassword("password123")
                .build();

        Role userRole = Role.builder().id(1L).name("ROLE_USER").build();
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .build();

        when(userRepository.existsByEmailIgnoreCase(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encodedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mock(UserDetails.class));
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("jwtToken");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwtToken", response.getToken());
        assertEquals("john@example.com", response.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegisterFailsWhenEmailExists() {
        // Arrange
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .confirmPassword("password123")
                .build();

        when(userRepository.existsByEmailIgnoreCase(registerRequest.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.register(registerRequest));
    }

    @Test
    void testRegisterFailsWhenPasswordsDoNotMatch() {
        // Arrange
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .confirmPassword("password456")
                .build();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> authService.register(registerRequest));
    }

    @Test
    void testForgotPasswordStoresTokenAndLogsNotification() {
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .build();

        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.requestPasswordReset(new ForgotPasswordRequest("john@example.com"));

        assertNotNull(user.getResetPasswordToken());
        assertNotNull(user.getResetPasswordTokenExpiresAt());
        verify(passwordResetNotificationService).sendPasswordResetEmail(eq(user), anyString(), any(LocalDateTime.class));
    }

    @Test
    void testResetPasswordUpdatesPasswordAndClearsToken() {
        User user = User.builder()
                .email("john@example.com")
                .resetPasswordToken("reset-token")
                .resetPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(30))
                .build();

        when(userRepository.findByResetPasswordToken("reset-token")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpassword")).thenReturn("encoded-new-password");

        authService.resetPassword(new ResetPasswordRequest("reset-token", "newpassword", "newpassword"));

        assertEquals("encoded-new-password", user.getPassword());
        assertNull(user.getResetPasswordToken());
        assertNull(user.getResetPasswordTokenExpiresAt());
        verify(userRepository).save(user);
    }
}

