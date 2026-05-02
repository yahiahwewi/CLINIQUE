package com.example.demo.mapper;

import com.example.demo.dto.RoleDTO;
import com.example.demo.dto.UserDTO;
import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }

        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.getEnabled())
                .roles(user.getRoles().stream()
                        .map(this::toRoleDTO)
                        .collect(Collectors.toSet()))
                .approvalStatus(user.getApprovalStatus() == null ? "APPROVED" : user.getApprovalStatus().name())
                .requestedRole(user.getRequestedRole())
                .build();
    }

    private RoleDTO toRoleDTO(Role role) {
        return RoleDTO.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .build();
    }
}

