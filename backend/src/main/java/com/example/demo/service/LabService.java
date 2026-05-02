package com.example.demo.service;

import com.example.demo.dto.LabOrderDTO;
import com.example.demo.dto.LabResultRequest;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.LabOrder;
import com.example.demo.entity.LabResult;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.LabOrderRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LabService {

    private final LabOrderRepository labOrderRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public LabService(
            LabOrderRepository labOrderRepository,
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.labOrderRepository = labOrderRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<LabOrderDTO> findByAppointment(Long appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        ensureCanRead(currentUser(), appt);
        return labOrderRepository.findByAppointmentIdOrderByCreatedAtDesc(appointmentId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LabOrderDTO> findByPatient(Long patientId) {
        User current = currentUser();
        if (!current.getId().equals(patientId) && !isStaff(current)) {
            throw new AccessDeniedException("You can only view your own lab results.");
        }
        return labOrderRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public LabOrderDTO create(LabOrderDTO dto) {
        Appointment appt = appointmentRepository.findById(dto.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        User current = currentUser();
        ensureIsAssignedDoctor(current, appt);

        LabOrder order = LabOrder.builder()
                .appointment(appt)
                .patient(appt.getPatient())
                .doctor(appt.getDoctor())
                .testName(dto.getTestName())
                .instructions(dto.getInstructions())
                .status(LabOrder.Status.ORDERED)
                .build();

        LabOrder saved = labOrderRepository.save(order);

        auditService.record(
                "LAB_ORDER_CREATED",
                "LabOrder",
                saved.getId(),
                String.format("Dr. %s ordered '%s' for %s",
                        current.getLastName(), saved.getTestName(), saved.getPatient().getEmail())
        );

        return toDTO(saved);
    }

    @Transactional
    public LabOrderDTO startProgress(Long id) {
        LabOrder order = labOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lab order not found"));
        ensureCanFulfillLab(currentUser(), order);
        order.setStatus(LabOrder.Status.IN_PROGRESS);
        return toDTO(labOrderRepository.save(order));
    }

    @Transactional
    public LabOrderDTO uploadResult(Long id, LabResultRequest request) {
        LabOrder order = labOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lab order not found"));
        User current = currentUser();
        ensureCanFulfillLab(current, order);

        LabResult result = order.getResult();
        if (result == null) {
            result = LabResult.builder()
                    .labOrder(order)
                    .build();
        }
        result.setResultText(request.getResultText());
        result.setAbnormal(Boolean.TRUE.equals(request.getAbnormal()));
        result.setCompletedBy(current);
        result.setCompletedAt(LocalDateTime.now());
        order.setResult(result);
        order.setStatus(LabOrder.Status.COMPLETED);

        LabOrder saved = labOrderRepository.save(order);

        auditService.record(
                "LAB_RESULT_UPLOADED",
                "LabOrder",
                saved.getId(),
                String.format("'%s' result uploaded by %s%s",
                        saved.getTestName(), current.getEmail(),
                        Boolean.TRUE.equals(result.getAbnormal()) ? " (ABNORMAL)" : "")
        );

        notificationService.notify(
                saved.getDoctor(),
                "Lab result available",
                String.format("'%s' result for %s is ready%s.",
                        saved.getTestName(),
                        saved.getPatient().getFirstName() + " " + saved.getPatient().getLastName(),
                        Boolean.TRUE.equals(result.getAbnormal()) ? " — flagged abnormal" : ""),
                "/dashboard/appointments"
        );
        notificationService.notify(
                saved.getPatient(),
                "Lab result available",
                "Your '" + saved.getTestName() + "' result is now in your history.",
                "/dashboard/history"
        );

        return toDTO(saved);
    }

    private LabOrderDTO toDTO(LabOrder o) {
        LabResult r = o.getResult();
        return LabOrderDTO.builder()
                .id(o.getId())
                .appointmentId(o.getAppointment().getId())
                .patientId(o.getPatient().getId())
                .patientName(name(o.getPatient()))
                .doctorId(o.getDoctor().getId())
                .doctorName(name(o.getDoctor()))
                .testName(o.getTestName())
                .instructions(o.getInstructions())
                .status(o.getStatus())
                .resultText(r == null ? null : r.getResultText())
                .abnormal(r == null ? null : r.getAbnormal())
                .completedByName(r == null || r.getCompletedBy() == null ? null : name(r.getCompletedBy()))
                .completedAt(r == null ? null : r.getCompletedAt())
                .createdAt(o.getCreatedAt())
                .build();
    }

    private String name(User u) { return u.getFirstName() + " " + u.getLastName(); }

    private boolean hasRole(User user, String role) {
        return user.getRoles().stream().map(Role::getName).anyMatch(role::equals);
    }
    private boolean isStaff(User user) {
        return user.getRoles().stream().map(Role::getName).anyMatch(r -> !r.equals("ROLE_USER"));
    }
    private void ensureCanFulfillLab(User u, LabOrder order) {
        boolean isAdmin = hasRole(u, "ROLE_ADMIN");
        boolean isOrderingDoctor = order.getDoctor().getId().equals(u.getId());
        if (!isAdmin && !isOrderingDoctor) {
            throw new AccessDeniedException("Only the ordering doctor or an admin can update this lab order.");
        }
    }
    private void ensureCanRead(User user, Appointment appt) {
        if (!appt.getPatient().getId().equals(user.getId()) && !isStaff(user)) {
            throw new AccessDeniedException("Not allowed to view these lab orders.");
        }
    }
    private void ensureIsAssignedDoctor(User user, Appointment appt) {
        boolean isAdmin = hasRole(user, "ROLE_ADMIN");
        boolean isAssignedDoctor = hasRole(user, "ROLE_DOCTOR")
                && appt.getDoctor().getId().equals(user.getId());
        if (!isAdmin && !isAssignedDoctor) {
            throw new AccessDeniedException("Only the assigned doctor can order labs for this appointment.");
        }
    }
    private User currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmailIgnoreCase(a.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
}
