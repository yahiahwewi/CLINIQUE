package com.example.demo.service;

import com.example.demo.dto.AppointmentDTO;
import com.example.demo.dto.AppointmentMetaDTO;
import com.example.demo.dto.AppointmentRequest;
import com.example.demo.dto.UserOptionDTO;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.AppointmentStatus;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final DoctorAvailabilityService availabilityService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            DoctorAvailabilityService availabilityService,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.availabilityService = availabilityService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> getAppointmentsForCurrentUser() {
        User currentUser = getCurrentUser();
        List<Appointment> appointments;

        if (hasRole(currentUser, "ROLE_ADMIN")) {
            appointments = appointmentRepository.findAllByOrderByAppointmentDateTimeAsc();
        } else if (hasRole(currentUser, "ROLE_DOCTOR")) {
            appointments = appointmentRepository.findByDoctorIdOrderByAppointmentDateTimeAsc(currentUser.getId());
        } else if (hasRole(currentUser, "ROLE_NURSE")) {
            appointments = appointmentRepository.findByNurseIdOrderByAppointmentDateTimeAsc(currentUser.getId());
        } else {
            appointments = appointmentRepository.findByPatientIdOrderByAppointmentDateTimeAsc(currentUser.getId());
        }

        return appointments.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AppointmentMetaDTO getAppointmentMeta() {
        return AppointmentMetaDTO.builder()
                .doctors(userRepository.findEnabledUsersByRoleName("ROLE_DOCTOR").stream()
                        .map(this::toUserOption)
                        .collect(Collectors.toList()))
                .nurses(userRepository.findEnabledUsersByRoleName("ROLE_NURSE").stream()
                        .map(this::toUserOption)
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional
    public AppointmentDTO createAppointment(AppointmentRequest request) {
        User currentUser = getCurrentUser();
        ensureUserCanCreateAppointments(currentUser);

        User doctor = getUserWithRole(request.getDoctorId(), "ROLE_DOCTOR");
        validateSlot(doctor.getId(), request.getAppointmentDateTime(), null);

        Appointment appointment = Appointment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .appointmentDateTime(request.getAppointmentDateTime())
                .status(AppointmentStatus.PENDING)
                .patient(currentUser)
                .doctor(doctor)
                .nurse(request.getNurseId() == null ? null : getUserWithRole(request.getNurseId(), "ROLE_NURSE"))
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        auditService.record(
                "APPOINTMENT_CREATED",
                "Appointment",
                saved.getId(),
                String.format("%s booked '%s' with Dr. %s on %s",
                        currentUser.getFirstName(), saved.getTitle(),
                        doctor.getLastName(), saved.getAppointmentDateTime())
        );

        notificationService.notify(
                doctor,
                "New appointment request",
                String.format("%s %s booked '%s' on %s",
                        currentUser.getFirstName(), currentUser.getLastName(),
                        saved.getTitle(), formatWhen(saved.getAppointmentDateTime())),
                "/dashboard/appointments"
        );
        if (saved.getNurse() != null) {
            notificationService.notify(
                    saved.getNurse(),
                    "You've been assigned to an appointment",
                    String.format("'%s' on %s", saved.getTitle(), formatWhen(saved.getAppointmentDateTime())),
                    "/dashboard/appointments"
            );
        }

        return toDTO(saved);
    }

    @Transactional
    public AppointmentDTO updateAppointment(Long appointmentId, AppointmentRequest request) {
        User currentUser = getCurrentUser();
        Appointment appointment = getAppointment(appointmentId);

        if (!canEditAppointment(currentUser, appointment)) {
            throw new AccessDeniedException("You are not allowed to edit this appointment");
        }

        User doctor = getUserWithRole(request.getDoctorId(), "ROLE_DOCTOR");
        boolean slotChanged = !appointment.getAppointmentDateTime().equals(request.getAppointmentDateTime())
                || !appointment.getDoctor().getId().equals(doctor.getId());
        if (slotChanged) {
            validateSlot(doctor.getId(), request.getAppointmentDateTime(), appointment.getId());
        }

        appointment.setTitle(request.getTitle());
        appointment.setDescription(request.getDescription());
        appointment.setAppointmentDateTime(request.getAppointmentDateTime());
        appointment.setDoctor(doctor);
        appointment.setNurse(request.getNurseId() == null ? null : getUserWithRole(request.getNurseId(), "ROLE_NURSE"));

        Appointment saved = appointmentRepository.save(appointment);

        auditService.record(
                "APPOINTMENT_UPDATED",
                "Appointment",
                saved.getId(),
                String.format("'%s' updated by %s", saved.getTitle(), currentUser.getEmail())
        );

        return toDTO(saved);
    }

    @Transactional
    public AppointmentDTO updateStatus(Long appointmentId, AppointmentStatus status) {
        User currentUser = getCurrentUser();
        Appointment appointment = getAppointment(appointmentId);

        if (!canManageStatus(currentUser, appointment)) {
            throw new AccessDeniedException("You are not allowed to change this appointment status");
        }

        AppointmentStatus previous = appointment.getStatus();
        appointment.setStatus(status);
        Appointment saved = appointmentRepository.save(appointment);

        auditService.record(
                "APPOINTMENT_STATUS_CHANGED",
                "Appointment",
                saved.getId(),
                String.format("'%s' %s -> %s by %s",
                        saved.getTitle(), previous, status, currentUser.getEmail())
        );

        notificationService.notify(
                saved.getPatient(),
                "Appointment " + status.name().toLowerCase(),
                String.format("Your appointment '%s' on %s was %s by Dr. %s",
                        saved.getTitle(), formatWhen(saved.getAppointmentDateTime()),
                        status.name().toLowerCase(), saved.getDoctor().getLastName()),
                "/dashboard/appointments"
        );

        return toDTO(saved);
    }

    @Transactional
    public void deleteAppointment(Long appointmentId) {
        User currentUser = getCurrentUser();
        Appointment appointment = getAppointment(appointmentId);

        if (!canDeleteAppointment(currentUser, appointment)) {
            throw new AccessDeniedException("You are not allowed to delete this appointment");
        }

        Long id = appointment.getId();
        String title = appointment.getTitle();
        User patient = appointment.getPatient();
        User doctor = appointment.getDoctor();

        appointmentRepository.delete(appointment);

        auditService.record(
                "APPOINTMENT_DELETED",
                "Appointment",
                id,
                String.format("'%s' deleted by %s", title, currentUser.getEmail())
        );

        if (!currentUser.getId().equals(patient.getId())) {
            notificationService.notify(
                    patient,
                    "Appointment cancelled",
                    "Your appointment '" + title + "' was cancelled.",
                    "/dashboard/appointments"
            );
        }
        if (!currentUser.getId().equals(doctor.getId())) {
            notificationService.notify(
                    doctor,
                    "Appointment cancelled",
                    "Appointment '" + title + "' was cancelled.",
                    "/dashboard/appointments"
            );
        }
    }

    @Transactional(readOnly = true)
    public long countAllAppointments() {
        return appointmentRepository.count();
    }

    @Transactional(readOnly = true)
    public long countPendingAppointments() {
        return appointmentRepository.countByStatus(AppointmentStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public long countAcceptedAppointments() {
        return appointmentRepository.countByStatus(AppointmentStatus.ACCEPTED);
    }

    private void validateSlot(Long doctorId, java.time.LocalDateTime start, Long excludeAppointmentId) {
        if (!availabilityService.isValidSlot(doctorId, start)) {
            throw new IllegalArgumentException(
                    "The chosen time is not an available slot for this doctor. Please pick from the proposed slots.");
        }
        if (availabilityService.isSlotTaken(doctorId, start, excludeAppointmentId)) {
            throw new IllegalArgumentException("That slot was just taken. Please pick another.");
        }
    }

    private String formatWhen(java.time.LocalDateTime when) {
        return when.toString().replace('T', ' ');
    }

    private void ensureUserCanCreateAppointments(User user) {
        if (!(hasRole(user, "ROLE_USER") || hasRole(user, "ROLE_ADMIN"))) {
            throw new AccessDeniedException("Only users and admins can create appointments");
        }
    }

    private boolean canEditAppointment(User user, Appointment appointment) {
        return hasRole(user, "ROLE_ADMIN")
                || appointment.getPatient().getId().equals(user.getId());
    }

    private boolean canManageStatus(User user, Appointment appointment) {
        return hasRole(user, "ROLE_ADMIN")
                || (hasRole(user, "ROLE_DOCTOR") && appointment.getDoctor().getId().equals(user.getId()));
    }

    private boolean canDeleteAppointment(User user, Appointment appointment) {
        return hasRole(user, "ROLE_ADMIN")
                || appointment.getPatient().getId().equals(user.getId())
                || (hasRole(user, "ROLE_DOCTOR") && appointment.getDoctor().getId().equals(user.getId()));
    }

    private Appointment getAppointment(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private User getUserWithRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (!hasRole(user, roleName)) {
            throw new IllegalArgumentException("Selected user does not have required role: " + roleName);
        }

        return user;
    }

    private boolean hasRole(User user, String roleName) {
        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return roles.contains(roleName);
    }

    private AppointmentDTO toDTO(Appointment appointment) {
        return AppointmentDTO.builder()
                .id(appointment.getId())
                .title(appointment.getTitle())
                .description(appointment.getDescription())
                .appointmentDateTime(appointment.getAppointmentDateTime())
                .status(appointment.getStatus())
                .patient(toUserOption(appointment.getPatient()))
                .doctor(toUserOption(appointment.getDoctor()))
                .nurse(appointment.getNurse() == null ? null : toUserOption(appointment.getNurse()))
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }

    private UserOptionDTO toUserOption(User user) {
        return UserOptionDTO.builder()
                .id(user.getId())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .build();
    }
}
