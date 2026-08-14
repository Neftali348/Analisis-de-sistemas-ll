package com.umg.sgq.entidad;

import com.umg.sgq.enumeracion.EstadoEvidencia;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "evidences")
@Getter
@Setter
public class Evidencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private Caso complaintCase;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follow_up_id")
    private Seguimiento followUp;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private Usuario uploadedBy;
    @Column(nullable = false, length = 255)
    private String originalName;
    @Column(nullable = false, unique = true, length = 255)
    private String storedName;
    @Column(nullable = false, length = 100)
    private String contentType;
    @Column(nullable = false)
    private long sizeBytes;
    @Column(nullable = false, length = 600)
    private String storagePath;
    @Column(nullable = false, length = 64)
    private String sha256;
    @Column(length = 500)
    private String description;
    @Column(nullable = false)
    private boolean visibleToClient = true;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEvidencia status = EstadoEvidencia.ACTIVA;
    @Column(length = 1000)
    private String annulReason;
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
    private LocalDateTime annulledAt;

    @PrePersist
    void prePersist() {
        uploadedAt = LocalDateTime.now();
    }
}
