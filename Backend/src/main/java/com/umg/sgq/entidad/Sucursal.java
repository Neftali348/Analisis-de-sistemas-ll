package com.umg.sgq.entidad;

import com.umg.sgq.enumeracion.EstadoRegistro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "branches", uniqueConstraints = @UniqueConstraint(name = "uk_branch_code", columnNames = "code"))
@Getter
@Setter
public class Sucursal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 10)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 250)
    private String address;
    @Column(nullable = false, length = 100)
    private String department;
    @Column(nullable = false, length = 100)
    private String municipality;
    @Column(length = 250)
    private String locationReference;
    @Column(length = 120)
    private String businessHours;
    @Column(length = 1000)
    private String observations;
    @Column(length = 20)
    private String phone;
    @Column(length = 150)
    private String email;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRegistro status = EstadoRegistro.ACTIVO;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private Usuario supervisor;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Version
    private long version;
}
