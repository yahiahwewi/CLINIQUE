package com.example.demo.service;

import com.example.demo.dto.PatientProfileDTO;
import com.example.demo.entity.PatientProfile;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.PatientProfileRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientProfileService {

    private final PatientProfileRepository profileRepository;
    private final UserRepository userRepository;

    public PatientProfileService(PatientProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PatientProfileDTO getMine() {
        User current = currentUser();
        return getByUserId(current.getId());
    }

    @Transactional(readOnly = true)
    public PatientProfileDTO getByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .map(this::toDTO)
                .orElseGet(() -> emptyProfileFor(userId));
    }

    @Transactional
    public PatientProfileDTO updateMine(PatientProfileDTO dto) {
        User current = currentUser();
        ensureIsPatientOrAdmin(current);
        return upsert(current.getId(), dto);
    }

    @Transactional
    public PatientProfileDTO upsert(Long userId, PatientProfileDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        PatientProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> PatientProfile.builder().user(user).build());

        profile.setDateOfBirth(dto.getDateOfBirth());
        profile.setGender(dto.getGender());
        profile.setBloodType(dto.getBloodType());
        profile.setAllergies(dto.getAllergies());
        profile.setChronicConditions(dto.getChronicConditions());
        profile.setEmergencyContactName(dto.getEmergencyContactName());
        profile.setEmergencyContactPhone(dto.getEmergencyContactPhone());

        return toDTO(profileRepository.save(profile));
    }

    private PatientProfileDTO emptyProfileFor(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return PatientProfileDTO.builder()
                .userId(userId)
                .fullName(user == null ? "" : user.getFirstName() + " " + user.getLastName())
                .email(user == null ? "" : user.getEmail())
                .build();
    }

    private PatientProfileDTO toDTO(PatientProfile p) {
        User u = p.getUser();
        return PatientProfileDTO.builder()
                .id(p.getId())
                .userId(u.getId())
                .fullName(u.getFirstName() + " " + u.getLastName())
                .email(u.getEmail())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender())
                .bloodType(p.getBloodType())
                .allergies(p.getAllergies())
                .chronicConditions(p.getChronicConditions())
                .emergencyContactName(p.getEmergencyContactName())
                .emergencyContactPhone(p.getEmergencyContactPhone())
                .build();
    }

    private User currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmailIgnoreCase(a.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }

    private void ensureIsPatientOrAdmin(User user) {
        boolean ok = user.getRoles().stream().map(Role::getName)
                .anyMatch(r -> r.equals("ROLE_USER") || r.equals("ROLE_ADMIN"));
        if (!ok) {
            throw new AccessDeniedException("Only patients can edit their own profile.");
        }
    }
}
