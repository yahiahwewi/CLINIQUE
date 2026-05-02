package com.example.demo.repository;

import com.example.demo.entity.Prescription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    @EntityGraph(attributePaths = {"items", "patient", "doctor", "appointment"})
    List<Prescription> findByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);

    @EntityGraph(attributePaths = {"items", "patient", "doctor"})
    List<Prescription> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    @EntityGraph(attributePaths = {"items", "patient", "doctor"})
    List<Prescription> findByStatusOrderByCreatedAtDesc(Prescription.Status status);

    long countByStatus(Prescription.Status status);
}
