package com.example.demo.repository;

import com.example.demo.entity.TimeOff;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimeOffRepository extends JpaRepository<TimeOff, Long> {

    @EntityGraph(attributePaths = {"doctor", "decidedBy"})
    List<TimeOff> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);

    @EntityGraph(attributePaths = {"doctor", "decidedBy"})
    List<TimeOff> findByStatusOrderByCreatedAtDesc(TimeOff.Status status);

    @EntityGraph(attributePaths = {"doctor"})
    List<TimeOff> findByDoctorIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long doctorId, TimeOff.Status status, LocalDate before, LocalDate after);

    long countByStatus(TimeOff.Status status);
}
