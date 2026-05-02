package com.example.demo.service;

import com.example.demo.dto.AnnouncementDTO;
import com.example.demo.entity.Announcement;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.AnnouncementRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository, UserRepository userRepository) {
        this.announcementRepository = announcementRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDTO> findAll() {
        return announcementRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    /** Active announcements visible to the currently authenticated user. */
    @Transactional(readOnly = true)
    public List<AnnouncementDTO> findActiveForCurrentUser() {
        boolean isStaff = isStaff(currentUserRoles());
        Announcement.Audience targeted = isStaff ? Announcement.Audience.STAFF : Announcement.Audience.PATIENTS;
        return announcementRepository.findActiveForAudience(
                LocalDateTime.now(), Announcement.Audience.ALL, targeted)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public AnnouncementDTO create(AnnouncementDTO dto) {
        Announcement a = Announcement.builder()
                .title(dto.getTitle())
                .body(dto.getBody())
                .audience(dto.getAudience() == null ? Announcement.Audience.ALL : dto.getAudience())
                .tone(dto.getTone() == null ? Announcement.Tone.INFO : dto.getTone())
                .active(dto.getActive() == null ? Boolean.TRUE : dto.getActive())
                .expiresAt(dto.getExpiresAt())
                .build();
        return toDTO(announcementRepository.save(a));
    }

    @Transactional
    public AnnouncementDTO update(Long id, AnnouncementDTO dto) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        a.setTitle(dto.getTitle());
        a.setBody(dto.getBody());
        if (dto.getAudience() != null) a.setAudience(dto.getAudience());
        if (dto.getTone() != null) a.setTone(dto.getTone());
        if (dto.getActive() != null) a.setActive(dto.getActive());
        a.setExpiresAt(dto.getExpiresAt());
        return toDTO(announcementRepository.save(a));
    }

    @Transactional
    public void delete(Long id) {
        if (!announcementRepository.existsById(id)) {
            throw new ResourceNotFoundException("Announcement not found");
        }
        announcementRepository.deleteById(id);
    }

    private List<String> currentUserRoles() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return List.of();
        return userRepository.findByEmailIgnoreCase(a.getName())
                .map(u -> u.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .orElseGet(List::of);
    }

    private boolean isStaff(List<String> roles) {
        return roles.stream().anyMatch(r -> !r.equals("ROLE_USER"));
    }

    private AnnouncementDTO toDTO(Announcement a) {
        return AnnouncementDTO.builder()
                .id(a.getId())
                .title(a.getTitle())
                .body(a.getBody())
                .audience(a.getAudience())
                .tone(a.getTone())
                .active(a.getActive())
                .createdAt(a.getCreatedAt())
                .expiresAt(a.getExpiresAt())
                .build();
    }
}
