package com.example.demo.repository;

import com.example.demo.entity.LabOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabOrderRepository extends JpaRepository<LabOrder, Long> {

    @EntityGraph(attributePaths = {"appointment", "patient", "doctor", "result"})
    List<LabOrder> findByAppointmentIdOrderByCreatedAtDesc(Long appointmentId);

    @EntityGraph(attributePaths = {"appointment", "patient", "doctor", "result"})
    List<LabOrder> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    @EntityGraph(attributePaths = {"appointment", "patient", "doctor", "result"})
    List<LabOrder> findByStatusOrderByCreatedAtAsc(LabOrder.Status status);

    long countByStatus(LabOrder.Status status);
}
