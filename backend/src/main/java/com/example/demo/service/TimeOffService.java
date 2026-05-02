package com.example.demo.service;

import com.example.demo.dto.TimeOffDTO;
import com.example.demo.entity.Role;
import com.example.demo.entity.TimeOff;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.TimeOffRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TimeOffService {

    private final TimeOffRepository repository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public TimeOffService(
            TimeOffRepository repository,
            UserRepository userRepository,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<TimeOffDTO> getMine() {
        User doctor = currentDoctor();
        return repository.findByDoctorIdOrderByCreatedAtDesc(doctor.getId()).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TimeOffDTO> findByStatus(TimeOff.Status status) {
        return repository.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return repository.countByStatus(TimeOff.Status.PENDING);
    }

    @Transactional
    public TimeOffDTO request(TimeOffDTO dto) {
        User doctor = currentDoctor();
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new IllegalArgumentException("Start date must be on or before the end date.");
        }
        TimeOff t = TimeOff.builder()
                .doctor(doctor)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .reason(dto.getReason())
                .status(TimeOff.Status.PENDING)
                .build();
        TimeOff saved = repository.save(t);

        auditService.record(
                "TIME_OFF_REQUESTED",
                "TimeOff",
                saved.getId(),
                String.format("Dr. %s requested %s → %s",
                        doctor.getLastName(), saved.getStartDate(), saved.getEndDate())
        );
        return toDTO(saved);
    }

    @Transactional
    public TimeOffDTO approve(Long id, String note) {
        return decide(id, TimeOff.Status.APPROVED, note);
    }

    @Transactional
    public TimeOffDTO reject(Long id, String note) {
        return decide(id, TimeOff.Status.REJECTED, note);
    }

    private TimeOffDTO decide(Long id, TimeOff.Status newStatus, String note) {
        TimeOff t = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Time-off request not found"));
        User admin = currentUser();

        t.setStatus(newStatus);
        t.setDecidedAt(LocalDateTime.now());
        t.setDecidedBy(admin);
        t.setDecisionNote(note);

        TimeOff saved = repository.save(t);

        auditService.record(
                "TIME_OFF_" + newStatus.name(),
                "TimeOff",
                saved.getId(),
                String.format("%s — Dr. %s, %s → %s",
                        newStatus.name(), saved.getDoctor().getLastName(),
                        saved.getStartDate(), saved.getEndDate())
        );

        notificationService.notify(
                saved.getDoctor(),
                "Time-off " + newStatus.name().toLowerCase(),
                String.format("Your request for %s → %s has been %s.",
                        saved.getStartDate(), saved.getEndDate(), newStatus.name().toLowerCase()),
                "/dashboard/time-off"
        );

        return toDTO(saved);
    }

    public boolean isDoctorOnApprovedLeave(Long doctorId, LocalDate date) {
        return !repository.findByDoctorIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                doctorId, TimeOff.Status.APPROVED, date, date).isEmpty();
    }

    private User currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmailIgnoreCase(a.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private User currentDoctor() {
        User u = currentUser();
        boolean isDoctor = u.getRoles().stream().map(Role::getName).anyMatch("ROLE_DOCTOR"::equals);
        if (!isDoctor) {
            throw new AccessDeniedException("Only doctors can request time off.");
        }
        return u;
    }

    private TimeOffDTO toDTO(TimeOff t) {
        return TimeOffDTO.builder()
                .id(t.getId())
                .doctorId(t.getDoctor().getId())
                .doctorName(t.getDoctor().getFirstName() + " " + t.getDoctor().getLastName())
                .doctorEmail(t.getDoctor().getEmail())
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .reason(t.getReason())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .decidedAt(t.getDecidedAt())
                .decidedByName(t.getDecidedBy() == null ? null : t.getDecidedBy().getFirstName() + " " + t.getDecidedBy().getLastName())
                .decisionNote(t.getDecisionNote())
                .build();
    }
}
