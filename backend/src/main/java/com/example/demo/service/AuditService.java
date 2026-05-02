package com.example.demo.service;

import com.example.demo.dto.AuditLogDTO;
import com.example.demo.entity.AuditLog;
import com.example.demo.entity.User;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void record(String action, String entityType, Long entityId, String summary) {
        AuditLog log = AuditLog.builder()
                .actor(currentActor())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .summary(summary)
                .build();
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLogDTO> getRecentLogs(int limit) {
        Page<AuditLog> page = auditLogRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(0, Math.min(limit, 200), Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return page.getContent().stream().map(this::toDTO).collect(Collectors.toList());
    }

    private User currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return userRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
    }

    private AuditLogDTO toDTO(AuditLog log) {
        User actor = log.getActor();
        return AuditLogDTO.builder()
                .id(log.getId())
                .actorName(actor == null ? "system" : actor.getFirstName() + " " + actor.getLastName())
                .actorEmail(actor == null ? null : actor.getEmail())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .summary(log.getSummary())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
