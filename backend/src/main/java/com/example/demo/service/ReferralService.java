package com.example.demo.service;

import com.example.demo.dto.ReferralDTO;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.Referral;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.ReferralRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public ReferralService(
            ReferralRepository referralRepository,
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.referralRepository = referralRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<ReferralDTO> incoming() {
        User me = currentDoctor();
        return referralRepository.findByToDoctorIdOrderByCreatedAtDesc(me.getId()).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReferralDTO> outgoing() {
        User me = currentDoctor();
        return referralRepository.findByFromDoctorIdOrderByCreatedAtDesc(me.getId()).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReferralDTO> forPatient(Long patientId) {
        User current = currentUser();
        if (!current.getId().equals(patientId) && !isStaff(current)) {
            throw new AccessDeniedException("You can only view your own referrals.");
        }
        return referralRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public ReferralDTO create(ReferralDTO dto) {
        User from = currentDoctor();
        User to = userRepository.findById(dto.getToDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Referred-to doctor not found"));
        if (!hasRole(to, "ROLE_DOCTOR")) {
            throw new IllegalArgumentException("Referred-to user must be a doctor.");
        }
        User patient = userRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        Appointment appt = null;
        if (dto.getAppointmentId() != null) {
            appt = appointmentRepository.findById(dto.getAppointmentId())
                    .orElse(null);
        }

        Referral ref = Referral.builder()
                .fromDoctor(from)
                .toDoctor(to)
                .patient(patient)
                .appointment(appt)
                .reason(dto.getReason())
                .status(Referral.Status.PENDING)
                .build();

        Referral saved = referralRepository.save(ref);

        auditService.record(
                "REFERRAL_CREATED",
                "Referral",
                saved.getId(),
                String.format("Dr. %s → Dr. %s for %s",
                        from.getLastName(), to.getLastName(), patient.getEmail())
        );

        notificationService.notify(
                to,
                "New referral",
                "Dr. " + from.getLastName() + " referred " + patient.getFirstName() + " " + patient.getLastName() + " to you.",
                "/dashboard/referrals"
        );

        return toDTO(saved);
    }

    @Transactional
    public ReferralDTO setStatus(Long id, Referral.Status status) {
        Referral ref = referralRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Referral not found"));
        User current = currentDoctor();
        if (!ref.getToDoctor().getId().equals(current.getId())) {
            throw new AccessDeniedException("Only the referred-to doctor can update this referral.");
        }
        ref.setStatus(status);
        Referral saved = referralRepository.save(ref);

        auditService.record(
                "REFERRAL_" + status.name(),
                "Referral",
                saved.getId(),
                "by Dr. " + current.getLastName()
        );

        notificationService.notify(
                ref.getFromDoctor(),
                "Referral " + status.name().toLowerCase(),
                "Dr. " + current.getLastName() + " " + status.name().toLowerCase() + " your referral.",
                "/dashboard/referrals"
        );

        return toDTO(saved);
    }

    private ReferralDTO toDTO(Referral r) {
        return ReferralDTO.builder()
                .id(r.getId())
                .fromDoctorId(r.getFromDoctor().getId())
                .fromDoctorName(name(r.getFromDoctor()))
                .toDoctorId(r.getToDoctor().getId())
                .toDoctorName(name(r.getToDoctor()))
                .patientId(r.getPatient().getId())
                .patientName(name(r.getPatient()))
                .appointmentId(r.getAppointment() == null ? null : r.getAppointment().getId())
                .reason(r.getReason())
                .status(r.getStatus())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private String name(User u) { return u.getFirstName() + " " + u.getLastName(); }
    private boolean hasRole(User user, String role) {
        return user.getRoles().stream().map(Role::getName).anyMatch(role::equals);
    }
    private boolean isStaff(User user) {
        return user.getRoles().stream().map(Role::getName).anyMatch(r -> !r.equals("ROLE_USER"));
    }
    private User currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmailIgnoreCase(a.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
    private User currentDoctor() {
        User u = currentUser();
        if (!hasRole(u, "ROLE_DOCTOR")) throw new AccessDeniedException("Only doctors can use referrals.");
        return u;
    }
}
