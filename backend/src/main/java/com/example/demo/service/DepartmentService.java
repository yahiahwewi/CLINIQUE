package com.example.demo.service;

import com.example.demo.dto.DepartmentDTO;
import com.example.demo.entity.Department;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.DepartmentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentDTO> findAll() {
        return departmentRepository.findAll(Sort.by("name")).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public DepartmentDTO create(DepartmentDTO dto) {
        if (departmentRepository.existsByNameIgnoreCase(dto.getName().trim())) {
            throw new IllegalArgumentException("A department with that name already exists.");
        }
        Department d = Department.builder()
                .name(dto.getName().trim())
                .description(dto.getDescription())
                .color(dto.getColor())
                .icon(dto.getIcon())
                .build();
        return toDTO(departmentRepository.save(d));
    }

    @Transactional
    public DepartmentDTO update(Long id, DepartmentDTO dto) {
        Department d = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        d.setName(dto.getName().trim());
        d.setDescription(dto.getDescription());
        d.setColor(dto.getColor());
        d.setIcon(dto.getIcon());
        return toDTO(departmentRepository.save(d));
    }

    @Transactional
    public void delete(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found");
        }
        departmentRepository.deleteById(id);
    }

    private DepartmentDTO toDTO(Department d) {
        return DepartmentDTO.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .color(d.getColor())
                .icon(d.getIcon())
                .build();
    }
}
