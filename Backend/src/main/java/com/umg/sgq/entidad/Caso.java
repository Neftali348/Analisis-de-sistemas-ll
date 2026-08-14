package com.umg.sgq.entidad;

import com.umg.sgq.enumeracion.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "cases", indexes = {@Index(name = "idx_case_code", columnList = "code"), @Index(name = "idx_case_status", columnList = "status"), @Index(name = "idx_case_branch", columnList = "branch_id")})
@Getter
@Setter
public class Caso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 20)
    private String code;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCaso type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoCaso status = EstadoCaso.REGISTRADO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridad priority;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaCaso category;
    @Column(nullable = false)
    private boolean anonymous;
    @Column(nullable = false)
    private boolean confidential;
    @Column(length = 150)
    private String fullName;
    @Column(length = 150)
    private String email;
    @Column(length = 20)
    private String phone;
    @Column(length = 80)
    private String trackingKeyHash;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Sucursal branch;
    @Column(length = 30)
    private String orderNumber;
    @Column(length = 1000)
    private String administrativeObservation;
    @Column(nullable = false)
    private LocalDateTime incidentAt;
    @Column(nullable = false, length = 3000)
    private String description;
    @Column(nullable = false)
    private boolean contactAuthorized;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_id")
    private Usuario responsible;
    @Column(length = 3000)
    private String resolution;
    private LocalDateTime resolutionAt;
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private MotivoCierre closeReason;
    @Column(length = 1000)
    private String closeComment;
    @Column(length = 1000)
    private String closeInternalObservation;
    @Column(length = 20)
    private String duplicateCaseCode;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private Usuario resolvedBy;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private Usuario closedBy;
    private LocalDateTime closedAt;
    private LocalDateTime reopenedAt;
    @Column(length = 1000)
    private String reopenReason;
    private LocalDateTime reopenRequestedAt;
    @Column(length = 1000)
    private String reopenRequestReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime firstResponseAt;
    private LocalDateTime slaWarningAt;
    private LocalDateTime slaDeadlineAt;
    private LocalDateTime slaPauseStartedAt;
    @Column(nullable = false)
    private boolean slaWarningSent = false;
    @Column(nullable = false)
    private boolean slaBreached = false;
    @Column(nullable = false)
    private boolean criticalEscalated = false;
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
