package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "lab_results", uniqueConstraints = @UniqueConstraint(columnNames = "lab_order_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_order_id", nullable = false, unique = true)
    private LabOrder labOrder;

    @Column(nullable = false, length = 4000)
    private String resultText;

    @Column(nullable = false)
    @Builder.Default
    private Boolean abnormal = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completed_by_id")
    private User completedBy;

    @Column(nullable = false)
    private LocalDateTime completedAt;

    @PrePersist
    public void onCreate() {
        this.completedAt = LocalDateTime.now();
    }
}
