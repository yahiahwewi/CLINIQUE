package com.example.demo.service;

import com.example.demo.dto.DepartmentDTO;
import com.example.demo.dto.DoctorProfileDTO;
import com.example.demo.entity.Department;
import com.example.demo.entity.DoctorProfile;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.DepartmentRepository;
import com.example.demo.repository.DoctorProfileRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DoctorProfileService {

    private final DoctorProfileRepository profileRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    public DoctorProfileService(
            DoctorProfileRepository profileRepository,
            DepartmentRepository departmentRepository,
            UserRepository userRepository
    ) {
        this.profileRepository = profileRepository;
        this.departmentRepository = departmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DoctorProfileDTO> findAllPublic() {
        return profileRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getUser().getEnabled()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DoctorProfileDTO getByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .map(this::toDTO)
                .orElseGet(() -> emptyProfileFor(userId));
    }

    @Transactional(readOnly = true)
    public DoctorProfileDTO getMine() {
        User current = currentUser();
        ensureIsDoctor(current);
        return getByUserId(current.getId());
    }

    @Transactional
    public DoctorProfileDTO updateMine(DoctorProfileDTO dto) {
        User current = currentUser();
        ensureIsDoctor(current);
        return upsert(current.getId(), dto);
    }

    @Transactional
    public DoctorProfileDTO adminUpsert(Long userId, DoctorProfileDTO dto) {
        return upsert(userId, dto);
    }

    private DoctorProfileDTO upsert(Long userId, DoctorProfileDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        DoctorProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> DoctorProfile.builder().user(user).departments(new HashSet<>()).build());

        profile.setSpecialty(dto.getSpecialty());
        profile.setLicenseNumber(dto.getLicenseNumber());
        profile.setBio(dto.getBio());
        profile.setLanguages(dto.getLanguages());
        profile.setConsultationFeeCents(dto.getConsultationFeeCents());
        profile.setYearsExperience(dto.getYearsExperience());
        profile.setPhotoUrl(dto.getPhotoUrl());

        if (dto.getDepartmentIds() != null) {
            Set<Department> deps = dto.getDepartmentIds().stream()
                    .map(id -> departmentRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + id)))
                    .collect(Collectors.toCollection(HashSet::new));
            profile.setDepartments(deps);
        }

        return toDTO(profileRepository.save(profile));
    }

    private DoctorProfileDTO emptyProfileFor(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return DoctorProfileDTO.builder()
                .userId(userId)
                .fullName(user == null ? "" : user.getFirstName() + " " + user.getLastName())
                .email(user == null ? "" : user.getEmail())
                .departments(List.of())
                .departmentIds(Set.of())
                .build();
    }

    private DoctorProfileDTO toDTO(DoctorProfile p) {
        User u = p.getUser();
        return DoctorProfileDTO.builder()
                .id(p.getId())
                .userId(u.getId())
                .fullName(u.getFirstName() + " " + u.getLastName())
                .email(u.getEmail())
                .specialty(p.getSpecialty())
                .licenseNumber(p.getLicenseNumber())
                .bio(p.getBio())
                .languages(p.getLanguages())
                .consultationFeeCents(p.getConsultationFeeCents())
                .yearsExperience(p.getYearsExperience())
                .photoUrl(p.getPhotoUrl())
                .departmentIds(p.getDepartments().stream().map(Department::getId).collect(Collectors.toSet()))
                .departments(p.getDepartments().stream().map(d -> DepartmentDTO.builder()
                        .id(d.getId()).name(d.getName()).color(d.getColor()).icon(d.getIcon()).build())
                        .collect(Collectors.toList()))
                .build();
    }

    private User currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmailIgnoreCase(a.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private void ensureIsDoctor(User user) {
        boolean isDoctor = user.getRoles().stream().map(Role::getName).anyMatch("ROLE_DOCTOR"::equals);
        if (!isDoctor) {
            throw new AccessDeniedException("Only doctors can edit a doctor profile.");
        }
    }
}
