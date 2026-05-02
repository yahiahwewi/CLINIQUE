package com.example.demo.repository;

import com.example.demo.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {
    Optional<Holiday> findByDate(LocalDate date);
    boolean existsByDate(LocalDate date);
    List<Holiday> findByDateBetweenOrderByDateAsc(LocalDate start, LocalDate end);
    List<Holiday> findAllByOrderByDateAsc();
}
