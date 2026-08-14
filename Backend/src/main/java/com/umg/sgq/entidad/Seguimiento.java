package com.umg.sgq.entidad;

import com.umg.sgq.enumeracion.EstadoCaso;
import com.umg.sgq.enumeracion.TipoSeguimiento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "follow_ups")
@Getter
@Setter
public class Seguimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private Caso complaintCase;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Usuario author;
    @Column(nullable = false, length = 80)
    private String authorLabel = "CLIENTE";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoSeguimiento type;
    @Column(nullable = false, length = 3000)
    private String description;
    @Column(nullable = false)
    private boolean visibleToClient;
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private EstadoCaso resultingStatus;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
