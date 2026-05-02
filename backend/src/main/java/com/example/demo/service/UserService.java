package com.example.demo.service;

import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserStatisticsDTO;
import com.example.demo.dto.AdminUserCreateRequest;
import com.example.demo.entity.ApprovalStatus;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.UserMapper;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AppointmentService appointmentService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            AppointmentService appointmentService,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.appointmentService = appointmentService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    public UserDTO getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public UserDTO getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .map(userMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEnabled(userDTO.getEnabled());

        if (userDTO.getRoles() != null) {
            Set<Role> roles = userDTO.getRoles().stream()
                    .map(roleDTO -> roleRepository.findByName(roleDTO.getName())
                            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleDTO.getName())))
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        User updatedUser = userRepository.save(user);
        return userMapper.toDTO(updatedUser);
    }

    @Transactional
    public void changePassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
    }

    @Transactional
    public UserDTO createUser(AdminUserCreateRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("Email already in use");
        }

        Set<Role> roles = request.getRoles().stream()
                .map(roleName -> roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName)))
                .collect(Collectors.toSet());

        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .enabled(request.getEnabled())
                .roles(roles)
                .build();

        return userMapper.toDTO(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserStatisticsDTO getUserStatistics() {
        long total = userRepository.count();
        long active = userRepository.countByEnabled(true);
        long inactive = total - active;
        return UserStatisticsDTO.builder()
                .totalUsers(total)
                .activeUsers(active)
                .inactiveUsers(inactive)
                .pendingApprovals(userRepository.countByApprovalStatus(ApprovalStatus.PENDING))
                .totalAppointments(appointmentService.countAllAppointments())
                .pendingAppointments(appointmentService.countPendingAppointments())
                .acceptedAppointments(appointmentService.countAcceptedAppointments())
                .build();
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getPendingApprovals() {
        return userRepository.findByApprovalStatusOrderByIdDesc(ApprovalStatus.PENDING).stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countPendingApprovals() {
        return userRepository.countByApprovalStatus(ApprovalStatus.PENDING);
    }

    @Transactional
    public UserDTO approveUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getApprovalStatus() == ApprovalStatus.APPROVED) {
            return userMapper.toDTO(user);
        }

        // Grant the role they requested at signup, if any.
        if (user.getRequestedRole() != null && !user.getRequestedRole().isBlank()) {
            roleRepository.findByName(user.getRequestedRole()).ifPresent(role -> {
                Set<Role> roles = user.getRoles() == null ? new HashSet<>() : new HashSet<>(user.getRoles());
                roles.add(role);
                user.setRoles(roles);
            });
        }
        user.setApprovalStatus(ApprovalStatus.APPROVED);
        user.setEnabled(true);
        User saved = userRepository.save(user);

        auditService.record(
                "USER_APPROVED",
                "User",
                saved.getId(),
                String.format("Approved %s (%s) — granted %s",
                        saved.getEmail(),
                        saved.getRequestedRole() == null ? "USER" : saved.getRequestedRole(),
                        saved.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")))
        );

        notificationService.notify(
                saved,
                "Your account has been approved",
                "Welcome to Lumen Health. You can now sign in.",
                "/login"
        );

        return userMapper.toDTO(saved);
    }

    @Transactional
    public UserDTO rejectUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setApprovalStatus(ApprovalStatus.REJECTED);
        user.setEnabled(false);
        User saved = userRepository.save(user);

        auditService.record(
                "USER_REJECTED",
                "User",
                saved.getId(),
                String.format("Rejected %s (requested %s)",
                        saved.getEmail(),
                        saved.getRequestedRole() == null ? "USER" : saved.getRequestedRole())
        );

        return userMapper.toDTO(saved);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
