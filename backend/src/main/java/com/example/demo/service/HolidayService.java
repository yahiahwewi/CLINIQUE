package com.example.demo.service;

import com.example.demo.dto.HolidayDTO;
import com.example.demo.entity.Holiday;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.HolidayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class HolidayService {

    private final HolidayRepository repository;

    public HolidayService(HolidayRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<HolidayDTO> findAll() {
        return repository.findAllByOrderByDateAsc().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public HolidayDTO create(HolidayDTO dto) {
        if (repository.existsByDate(dto.getDate())) {
            throw new IllegalArgumentException("A holiday already exists for that date.");
        }
        Holiday h = Holiday.builder().date(dto.getDate()).name(dto.getName().trim()).build();
        return toDTO(repository.save(h));
    }

    @Transactional
    public HolidayDTO update(Long id, HolidayDTO dto) {
        Holiday h = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found"));
        if (!h.getDate().equals(dto.getDate()) && repository.existsByDate(dto.getDate())) {
            throw new IllegalArgumentException("A holiday already exists for that date.");
        }
        h.setDate(dto.getDate());
        h.setName(dto.getName().trim());
        return toDTO(repository.save(h));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Holiday not found");
        }
        repository.deleteById(id);
    }

    public boolean isHoliday(LocalDate date) {
        return repository.existsByDate(date);
    }

    private HolidayDTO toDTO(Holiday h) {
        return HolidayDTO.builder().id(h.getId()).date(h.getDate()).name(h.getName()).build();
    }
}
