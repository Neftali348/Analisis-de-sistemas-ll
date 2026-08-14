package com.umg.sgq.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "satisfaction", uniqueConstraints = @UniqueConstraint(name = "uk_satisfaction_case", columnNames = "case_id"))
@Getter
@Setter
public class Satisfaccion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private Caso complaintCase;
    @Column(nullable = false)
    private int rating;
    @Column(length = 1000)
    private String comment;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
