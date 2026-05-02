package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(length = 500)
    private String description;

    /** Hex color used for UI chips (e.g. #0F4C5C). */
    @Column(length = 16)
    private String color;

    /** Lucide-style icon name (e.g. "heart", "baby", "stethoscope"). */
    @Column(length = 32)
    private String icon;
}
