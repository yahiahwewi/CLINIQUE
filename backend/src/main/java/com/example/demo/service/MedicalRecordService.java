package com.example.demo.service;

import com.example.demo.dto.MedicalRecordDTO;
import com.example.demo.entity.Appointment;
import com.example.demo.entity.MedicalRecord;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.MedicalRecordRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MedicalRecordService {

    private final MedicalRecordRepository recordRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public MedicalRecordService(
            MedicalRecordRepository recordRepository,
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.recordRepository = recordRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public MedicalRecordDTO getByAppointmentId(Long appointmentId) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        ensureCanRead(currentUser(), appt);

        return recordRepository.findByAppointmentId(appointmentId)
                .map(r -> toDTO(r, true))
                .orElseGet(() -> emptyFor(appt));
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordDTO> getForPatient(Long patientId) {
        User current = currentUser();
        if (!current.getId().equals(patientId) && !isStaff(current)) {
            throw new AccessDeniedException("You can only view your own medical history.");
        }
        return recordRepository.findByAppointment_Patient_IdOrderByCreatedAtDesc(patientId).stream()
                .map(r -> toDTO(r, isStaff(current)))
                .collect(Collectors.toList());
    }

    @Transactional
    public MedicalRecordDTO upsert(Long appointmentId, MedicalRecordDTO dto) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        User current = currentUser();
        ensureCanWrite(current, appt);

        MedicalRecord record = recordRepository.findByAppointmentId(appointmentId)
                .orElseGet(() -> MedicalRecord.builder().appointment(appt).build());

        record.setChiefComplaint(dto.getChiefComplaint());
        record.setDiagnosis(dto.getDiagnosis());
        record.setPlan(dto.getPlan());
        record.setPrivateNotes(dto.getPrivateNotes());
        // patientSummary is set separately by setPatientSummary (AI flow)
        if (dto.getPatientSummary() != null) {
            record.setPatientSummary(dto.getPatientSummary());
        }

        MedicalRecord saved = recordRepository.save(record);

        auditService.record(
                "MEDICAL_RECORD_UPDATED",
                "MedicalRecord",
                saved.getId(),
                "by " + current.getEmail() + " for appt #" + appt.getId()
        );

        return toDTO(saved, true);
    }

    @Transactional
    public MedicalRecordDTO setPatientSummary(Long appointmentId, String summary) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
        ensureCanWrite(currentUser(), appt);

        MedicalRecord record = recordRepository.findByAppointmentId(appointmentId)
                .orElseGet(() -> MedicalRecord.builder().appointment(appt).build());
        record.setPatientSummary(summary);
        MedicalRecord saved = recordRepository.save(record);

        notificationService.notify(
                appt.getPatient(),
                "Visit summary available",
                "Your doctor shared a summary of your visit '" + appt.getTitle() + "'.",
                "/dashboard/history"
        );

        return toDTO(saved, true);
    }

    private MedicalRecordDTO toDTO(MedicalRecord r, boolean includePrivate) {
        Appointment appt = r.getAppointment();
        return MedicalRecordDTO.builder()
                .id(r.getId())
                .appointmentId(appt.getId())
                .patientName(appt.getPatient().getFirstName() + " " + appt.getPatient().getLastName())
                .doctorName(appt.getDoctor().getFirstName() + " " + appt.getDoctor().getLastName())
                .appointmentDateTime(appt.getAppointmentDateTime())
                .chiefComplaint(r.getChiefComplaint())
                .diagnosis(r.getDiagnosis())
                .plan(r.getPlan())
                .privateNotes(includePrivate ? r.getPrivateNotes() : null)
                .patientSummary(r.getPatientSummary())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private MedicalRecordDTO emptyFor(Appointment appt) {
        return MedicalRecordDTO.builder()
                .appointmentId(appt.getId())
                .patientName(appt.getPatient().getFirstName() + " " + appt.getPatient().getLastName())
                .doctorName(appt.getDoctor().getFirstName() + " " + appt.getDoctor().getLastName())
                .appointmentDateTime(appt.getAppointmentDateTime())
                .build();
    }

    private void ensureCanRead(User user, Appointment appt) {
        boolean own = appt.getPatient().getId().equals(user.getId());
        if (!own && !isStaff(user)) {
            throw new AccessDeniedException("Not allowed to view this medical record.");
        }
    }

    private void ensureCanWrite(User user, Appointment appt) {
        boolean isAdmin = hasRole(user, "ROLE_ADMIN");
        boolean isAssignedDoctor = hasRole(user, "ROLE_DOCTOR")
                && appt.getDoctor().getId().equals(user.getId());
        if (!isAdmin && !isAssignedDoctor) {
            throw new AccessDeniedException("Only the assigned doctor (or an admin) can edit this record.");
        }
    }

    private boolean isStaff(User user) {
        return user.getRoles().stream().map(Role::getName).anyMatch(r -> !r.equals("ROLE_USER"));
    }

    private boolean hasRole(User user, String role) {
        return user.getRoles().stream().map(Role::getName).anyMatch(role::equals);
    }

    private User currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmailIgnoreCase(a.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
}
