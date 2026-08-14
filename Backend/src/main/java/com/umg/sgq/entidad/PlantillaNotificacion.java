package com.umg.sgq.entidad;

import com.umg.sgq.enumeracion.CanalNotificacion;
import com.umg.sgq.enumeracion.EventoNotificacion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_templates", uniqueConstraints = @UniqueConstraint(name = "uk_template_event_channel", columnNames = {"event", "channel"}))
@Getter
@Setter
public class PlantillaNotificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventoNotificacion event;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalNotificacion channel;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(length = 250)
    private String subject;
    @Column(nullable = false, length = 4000)
    private String content;
    @Column(nullable = false)
    private boolean active = true;
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
}
