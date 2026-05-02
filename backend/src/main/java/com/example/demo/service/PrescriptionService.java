package com.example.demo.service;

import com.example.demo.dto.PrescriptionDTO;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.Prescription;
import com.example.demo.entity.PrescriptionItem;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.PrescriptionRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public PrescriptionService(
            PrescriptionRepository prescriptionRepository,
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.prescriptionRepository = prescriptionRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDTO> findByAppointment(Long appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        ensureCanRead(currentUser(), appt);
        return prescriptionRepository.findByAppointmentIdOrderByCreatedAtDesc(appointmentId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PrescriptionDTO> findByPatient(Long patientId) {
        User current = currentUser();
        if (!current.getId().equals(patientId) && !isStaff(current)) {
            throw new AccessDeniedException("You can only view your own prescriptions.");
        }
        return prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public PrescriptionDTO create(PrescriptionDTO dto) {
        Appointment appt = appointmentRepository.findById(dto.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        User current = currentUser();
        ensureIsAssignedDoctor(current, appt);

        Prescription rx = Prescription.builder()
                .appointment(appt)
                .patient(appt.getPatient())
                .doctor(appt.getDoctor())
                .instructions(dto.getInstructions())
                .status(dto.getStatus() == null ? Prescription.Status.ACTIVE : dto.getStatus())
                .items(new ArrayList<>())
                .build();

        if (dto.getItems() != null) {
            for (PrescriptionDTO.Item i : dto.getItems()) {
                rx.getItems().add(PrescriptionItem.builder()
                        .prescription(rx)
                        .drugName(i.getDrugName())
                        .dose(i.getDose())
                        .frequency(i.getFrequency())
                        .durationDays(i.getDurationDays())
                        .notes(i.getNotes())
                        .build());
            }
        }

        Prescription saved = prescriptionRepository.save(rx);

        auditService.record(
                "PRESCRIPTION_CREATED",
                "Prescription",
                saved.getId(),
                String.format("Dr. %s issued %d items for %s",
                        current.getLastName(), saved.getItems().size(),
                        saved.getPatient().getEmail())
        );

        notificationService.notify(
                saved.getPatient(),
                "New prescription",
                "Dr. " + saved.getDoctor().getLastName() + " issued you a new prescription.",
                "/dashboard/history"
        );

        return toDTO(saved);
    }

    @Transactional
    public PrescriptionDTO markDispensed(Long id) {
        Prescription rx = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
        User current = currentUser();
        boolean isPrescribingDoctor = rx.getDoctor().getId().equals(current.getId());
        if (!isPrescribingDoctor && !hasRole(current, "ROLE_ADMIN")) {
            throw new AccessDeniedException("Only the prescribing doctor or an admin can mark this dispensed.");
        }
        rx.setStatus(Prescription.Status.DISPENSED);
        rx.setDispensedBy(current);
        rx.setDispensedAt(LocalDateTime.now());
        Prescription saved = prescriptionRepository.save(rx);

        auditService.record(
                "PRESCRIPTION_DISPENSED",
                "Prescription",
                saved.getId(),
                "by " + current.getEmail()
        );

        notificationService.notify(
                saved.getPatient(),
                "Prescription dispensed",
                "Your prescription has been dispensed by " + current.getFirstName() + ".",
                "/dashboard/history"
        );

        return toDTO(saved);
    }

    @Transactional
    public void cancel(Long id) {
        Prescription rx = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
        User current = currentUser();
        if (!current.getId().equals(rx.getDoctor().getId()) && !hasRole(current, "ROLE_ADMIN")) {
            throw new AccessDeniedException("Only the prescribing doctor can cancel this prescription.");
        }
        rx.setStatus(Prescription.Status.CANCELLED);
        prescriptionRepository.save(rx);
        auditService.record("PRESCRIPTION_CANCELLED", "Prescription", id, "by " + current.getEmail());
    }

    @Transactional(readOnly = true)
    public Prescription getEntityForPdf(Long id) {
        Prescription rx = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));
        User current = currentUser();
        boolean isPatient = rx.getPatient().getId().equals(current.getId());
        if (!isPatient && !isStaff(current)) {
            throw new AccessDeniedException("Not allowed to view this prescription.");
        }
        // touch items for lazy loading just in case
        rx.getItems().size();
        return rx;
    }

    private PrescriptionDTO toDTO(Prescription rx) {
        return PrescriptionDTO.builder()
                .id(rx.getId())
                .appointmentId(rx.getAppointment().getId())
                .patientId(rx.getPatient().getId())
                .patientName(name(rx.getPatient()))
                .doctorId(rx.getDoctor().getId())
                .doctorName(name(rx.getDoctor()))
                .instructions(rx.getInstructions())
                .status(rx.getStatus())
                .dispensedByName(rx.getDispensedBy() == null ? null : name(rx.getDispensedBy()))
                .dispensedAt(rx.getDispensedAt())
                .items(rx.getItems().stream().map(i -> PrescriptionDTO.Item.builder()
                        .id(i.getId())
                        .drugName(i.getDrugName())
                        .dose(i.getDose())
                        .frequency(i.getFrequency())
                        .durationDays(i.getDurationDays())
                        .notes(i.getNotes())
                        .build()).collect(Collectors.toList()))
                .createdAt(rx.getCreatedAt())
                .build();
    }

    private String name(User u) { return u.getFirstName() + " " + u.getLastName(); }

    private boolean hasRole(User user, String role) {
        return user.getRoles().stream().map(Role::getName).anyMatch(role::equals);
    }

    private boolean isStaff(User user) {
        return user.getRoles().stream().map(Role::getName).anyMatch(r -> !r.equals("ROLE_USER"));
    }

    private void ensureCanRead(User user, Appointment appt) {
        if (!appt.getPatient().getId().equals(user.getId()) && !isStaff(user)) {
            throw new AccessDeniedException("Not allowed to view these prescriptions.");
        }
    }

    private void ensureIsAssignedDoctor(User user, Appointment appt) {
        boolean isAdmin = hasRole(user, "ROLE_ADMIN");
        boolean isAssignedDoctor = hasRole(user, "ROLE_DOCTOR")
                && appt.getDoctor().getId().equals(user.getId());
        if (!isAdmin && !isAssignedDoctor) {
            throw new AccessDeniedException("Only the assigned doctor can prescribe for this appointment.");
        }
    }

    private User currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmailIgnoreCase(a.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
}
