package com.example.demo.repository;

import com.example.demo.entity.MedicalRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    @EntityGraph(attributePaths = {"appointment", "appointment.patient", "appointment.doctor"})
    Optional<MedicalRecord> findByAppointmentId(Long appointmentId);

    @EntityGraph(attributePaths = {"appointment", "appointment.doctor"})
    List<MedicalRecord> findByAppointment_Patient_IdOrderByCreatedAtDesc(Long patientId);
}
