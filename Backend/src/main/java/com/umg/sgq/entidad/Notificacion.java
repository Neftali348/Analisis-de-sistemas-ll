package com.umg.sgq.entidad;

import com.umg.sgq.enumeracion.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
public class Notificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private Caso complaintCase;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id")
    private Usuario recipientUser;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventoNotificacion event;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalNotificacion channel;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNotificacion status = EstadoNotificacion.PENDIENTE;
    @Column(length = 180)
    private String recipient;
    @Column(length = 250)
    private String subject;
    @Column(nullable = false, length = 4000)
    private String content;
    @Column(nullable = false)
    private int attempts = 0;
    @Column(length = 800)
    private String lastError;
    @Column(length = 1000)
    private String cancellationReason;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_notification_id")
    private Notificacion originalNotification;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
