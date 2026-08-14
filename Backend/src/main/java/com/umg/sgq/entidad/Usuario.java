package com.umg.sgq.entidad;

import com.umg.sgq.enumeracion.EstadoRegistro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {@UniqueConstraint(name = "uk_user_username", columnNames = "username"), @UniqueConstraint(name = "uk_user_email", columnNames = "email")})
@Getter
@Setter
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String fullName;
    @Column(nullable = false, length = 30)
    private String username;
    @Column(nullable = false, length = 150)
    private String email;
    @Column(nullable = false, length = 255)
    private String passwordHash;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Rol role;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Sucursal branch;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRegistro status = EstadoRegistro.ACTIVO;
    @Column(nullable = false)
    private int failedAttempts = 0;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lastActivityAt;
    @Column(nullable = false)
    private boolean mustChangePassword = false;
    @Column(length = 64)
    private String passwordResetTokenHash;
    private LocalDateTime passwordResetExpiresAt;
    private LocalDateTime credentialsChangedAt;
    @Column(nullable = false)
    private long credentialVersion = 0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Version
    private long version;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
