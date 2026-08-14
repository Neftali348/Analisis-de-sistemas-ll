package com.umg.sgq.servicio;

import com.umg.sgq.entidad.Caso;
import com.umg.sgq.enumeracion.*;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
public class ServicioReglasCaso {
    private static final Map<EstadoCaso, Set<EstadoCaso>> ALLOWED = new EnumMap<>(EstadoCaso.class);

    static {
        ALLOWED.put(EstadoCaso.REGISTRADO, EnumSet.of(EstadoCaso.PENDIENTE_ASIGNACION, EstadoCaso.CANCELADO));
        ALLOWED.put(EstadoCaso.PENDIENTE_ASIGNACION, EnumSet.of(EstadoCaso.ASIGNADO, EstadoCaso.RECHAZADO, EstadoCaso.CANCELADO));
        ALLOWED.put(EstadoCaso.ASIGNADO, EnumSet.of(EstadoCaso.EN_PROCESO, EstadoCaso.RECHAZADO, EstadoCaso.CANCELADO));
        ALLOWED.put(EstadoCaso.EN_PROCESO, EnumSet.of(EstadoCaso.EN_ESPERA_CLIENTE, EstadoCaso.RESUELTO, EstadoCaso.RECHAZADO, EstadoCaso.CANCELADO));
        ALLOWED.put(EstadoCaso.EN_ESPERA_CLIENTE, EnumSet.of(EstadoCaso.EN_PROCESO, EstadoCaso.RESUELTO, EstadoCaso.CANCELADO));
        ALLOWED.put(EstadoCaso.RESUELTO, EnumSet.of(EstadoCaso.CERRADO, EstadoCaso.REABIERTO));
        ALLOWED.put(EstadoCaso.CERRADO, EnumSet.of(EstadoCaso.REABIERTO));
        ALLOWED.put(EstadoCaso.RECHAZADO, EnumSet.of(EstadoCaso.REABIERTO));
        ALLOWED.put(EstadoCaso.CANCELADO, EnumSet.of(EstadoCaso.REABIERTO));
        ALLOWED.put(EstadoCaso.REABIERTO, EnumSet.of(EstadoCaso.ASIGNADO, EstadoCaso.EN_PROCESO));
    }

    public void requireTransition(EstadoCaso from, EstadoCaso to) {
        if (from == to) return;
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to))
            throw new IllegalArgumentException("El cambio de estado solicitado no está permitido.");
    }

    public Prioridad defaultPriority(TipoCaso type) {
        switch (type) {
            case SUGERENCIA:
                return Prioridad.BAJA;
            case QUEJA:
                return Prioridad.MEDIA;
            case RECLAMO:
                return Prioridad.ALTA;
            case DENUNCIA:
                return Prioridad.CRITICA;
            default:
                throw new IllegalArgumentException("Tipo de caso no soportado: " + type);
        }
    }

    public Duration sla(Prioridad p) {
        switch (p) {
            case BAJA:
                return Duration.ofHours(72);
            case MEDIA:
                return Duration.ofHours(48);
            case ALTA:
                return Duration.ofHours(24);
            case CRITICA:
                return Duration.ofHours(4);
            default:
                throw new IllegalArgumentException("Prioridad no soportada: " + p);
        }
    }

    public void initializeSla(Caso c) {
        Duration d = sla(c.getPriority());

        c.setSlaDeadlineAt(
                c.getCreatedAt().plus(d)
        );

        Duration warning = d.multipliedBy(80).dividedBy(100);

        c.setSlaWarningAt(
                c.getCreatedAt().plus(warning)
        );
    }

    public void pauseSla(Caso c) {
        if (c.getSlaPauseStartedAt() == null) c.setSlaPauseStartedAt(LocalDateTime.now());
    }

    public void resumeSla(Caso c) {
        if (c.getSlaPauseStartedAt() != null) {
            Duration p = Duration.between(c.getSlaPauseStartedAt(), LocalDateTime.now());
            c.setSlaDeadlineAt(c.getSlaDeadlineAt().plus(p));
            c.setSlaWarningAt(c.getSlaWarningAt().plus(p));
            c.setSlaPauseStartedAt(null);
        }
    }

    public void applyTransitionSla(Caso c, EstadoCaso oldStatus, EstadoCaso newStatus) {
        if (oldStatus != EstadoCaso.EN_ESPERA_CLIENTE && newStatus == EstadoCaso.EN_ESPERA_CLIENTE) pauseSla(c);
        if (oldStatus == EstadoCaso.EN_ESPERA_CLIENTE && newStatus != EstadoCaso.EN_ESPERA_CLIENTE) resumeSla(c);
    }
}
