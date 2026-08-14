package com.umg.sgq.entidad;

import com.umg.sgq.enumeracion.ResultadoAuditoria;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {@Index(name = "idx_audit_created", columnList = "createdAt"), @Index(name = "idx_audit_module", columnList = "module")})
@Getter
@Setter
public class RegistroAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false, updatable = false, length = 80)
    private String username;
    @Column(nullable = false, updatable = false, length = 50)
    private String role;
    @Column(nullable = false, updatable = false, length = 80)
    private String ipAddress;
    @Column(nullable = false, updatable = false, length = 80)
    private String module;
    @Column(nullable = false, updatable = false, length = 100)
    private String action;
    @Column(updatable = false, length = 80)
    private String entityType;
    @Column(updatable = false, length = 100)
    private String entityReference;
    @Column(nullable = false, updatable = false, length = 1500)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private ResultadoAuditoria result;
    @Lob
    @Column(updatable = false)
    private String oldValues;
    @Lob
    @Column(updatable = false)
    private String newValues;
    @Column(updatable = false, length = 64)
    private String previousHash;
    @Column(nullable = false, updatable = false, length = 64)
    private String integrityHash;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
