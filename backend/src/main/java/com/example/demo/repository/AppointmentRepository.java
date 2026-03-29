package com.example.demo.repository;

import com.example.demo.entity.Appointment;
import com.example.demo.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    @EntityGraph(attributePaths = {"patient", "doctor", "nurse"})
    List<Appointment> findAllByOrderByAppointmentDateTimeAsc();

    @EntityGraph(attributePaths = {"patient", "doctor", "nurse"})
    List<Appointment> findByPatientIdOrderByAppointmentDateTimeAsc(Long patientId);

    @EntityGraph(attributePaths = {"patient", "doctor", "nurse"})
    List<Appointment> findByDoctorIdOrderByAppointmentDateTimeAsc(Long doctorId);

    @EntityGraph(attributePaths = {"patient", "doctor", "nurse"})
    List<Appointment> findByNurseIdOrderByAppointmentDateTimeAsc(Long nurseId);

    long countByStatus(AppointmentStatus status);
}
