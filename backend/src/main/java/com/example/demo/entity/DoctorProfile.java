package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "doctor_profiles", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "doctor_departments",
            joinColumns = @JoinColumn(name = "doctor_profile_id"),
            inverseJoinColumns = @JoinColumn(name = "department_id")
    )
    @Builder.Default
    private Set<Department> departments = new HashSet<>();

    @Column(length = 80)
    private String specialty;

    @Column(length = 60)
    private String licenseNumber;

    @Column(length = 2000)
    private String bio;

    /** Comma-separated list of languages (e.g. "English, French, Arabic"). */
    @Column(length = 200)
    private String languages;

    /** Display-only consultation fee in cents (e.g. 8000 = €80.00). No payment is processed. */
    private Integer consultationFeeCents;

    private Integer yearsExperience;

    @Column(length = 500)
    private String photoUrl;
}
