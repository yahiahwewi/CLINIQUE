package com.example.demo.repository;

import com.example.demo.entity.DoctorProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {

    @EntityGraph(attributePaths = {"user", "departments"})
    Optional<DoctorProfile> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "departments"})
    List<DoctorProfile> findAll();
}
