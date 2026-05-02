package com.example.demo.repository;

import com.example.demo.entity.Referral;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, Long> {

    @EntityGraph(attributePaths = {"fromDoctor", "toDoctor", "patient", "appointment"})
    List<Referral> findByToDoctorIdOrderByCreatedAtDesc(Long toDoctorId);

    @EntityGraph(attributePaths = {"fromDoctor", "toDoctor", "patient", "appointment"})
    List<Referral> findByFromDoctorIdOrderByCreatedAtDesc(Long fromDoctorId);

    @EntityGraph(attributePaths = {"fromDoctor", "toDoctor", "patient"})
    List<Referral> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}
