package com.umg.sgq.servicio;

import com.umg.sgq.entidad.*;
import com.umg.sgq.enumeracion.*;
import com.umg.sgq.repositorio.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ProgramadorSla {
    private final RepositorioCaso cases;
    private final RepositorioUsuario users;
    private final ServicioNotificaciones notifications;
    private final RepositorioNotificacion notificationRepo;
    private final ServicioAuditoria audit;

    public ProgramadorSla(RepositorioCaso cases, RepositorioUsuario users, ServicioNotificaciones notifications, RepositorioNotificacion notificationRepo, ServicioAuditoria audit) {
        this.cases = cases;
        this.users = users;
        this.notifications = notifications;
        this.notificationRepo = notificationRepo;
        this.audit = audit;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void check() {
        LocalDateTime now = LocalDateTime.now();
        List<Caso> pending = cases.findByFirstResponseAtIsNullAndStatusNotIn(List.of(EstadoCaso.CERRADO, EstadoCaso.RECHAZADO, EstadoCaso.CANCELADO));
        List<Usuario> admins = users.findActiveByRoleAndBranch(CodigoRol.ADMINISTRADOR, EstadoRegistro.ACTIVO, null);
        for (Caso c : pending) {
            if (c.getSlaWarningAt() != null && !c.isSlaWarningSent() && !now.isBefore(c.getSlaWarningAt())) {
                if (c.getResponsible() != null)
                    notifications.internal(EventoNotificacion.SLA_80, c, c.getResponsible(), "El caso alcanzó el 80% del SLA.");
                if (c.getBranch().getSupervisor() != null)
                    notifications.internal(EventoNotificacion.SLA_80, c, c.getBranch().getSupervisor(), "El caso " + c.getCode() + " alcanzó el 80% del SLA.");
                c.setSlaWarningSent(true);
                audit.logSystem("CASOS", "SLA_80", "CASO", c.getCode(), "Advertencia de SLA generada", ResultadoAuditoria.EXITOSO);
            }
            if (c.getSlaDeadlineAt() != null && !c.isSlaBreached() && now.isAfter(c.getSlaDeadlineAt())) {
                c.setSlaBreached(true);
                if (c.getBranch().getSupervisor() != null)
                    notifications.internal(EventoNotificacion.SLA_VENCIDO, c, c.getBranch().getSupervisor(), "El caso " + c.getCode() + " incumplió el SLA.");
                for (Usuario a : admins) {
                    notifications.internal(EventoNotificacion.SLA_VENCIDO, c, a, "El caso " + c.getCode() + " incumplió el SLA.");
                    notifications.email(EventoNotificacion.SLA_VENCIDO, c, a.getEmail(), "El caso " + c.getCode() + " incumplió el SLA.");
                }
                audit.logSystem("CASOS", "SLA_VENCIDO", "CASO", c.getCode(), "Incumplimiento de SLA detectado", ResultadoAuditoria.EXITOSO);
            }
            if (c.getPriority() == Prioridad.CRITICA && !c.isCriticalEscalated() && c.getCreatedAt() != null && now.isAfter(c.getCreatedAt().plusHours(2))) {
                for (Usuario a : admins) {
                    notifications.internal(EventoNotificacion.ESCALAMIENTO_CRITICO, c, a, "Caso crítico sin primera atención dentro de 2 horas: " + c.getCode());
                    notifications.email(EventoNotificacion.ESCALAMIENTO_CRITICO, c, a.getEmail(), "Caso crítico sin primera atención dentro de 2 horas: " + c.getCode());
                }
                c.setCriticalEscalated(true);
                audit.logSystem("CASOS", "ESCALAMIENTO_CRITICO", "CASO", c.getCode(), "Escalamiento automático de caso crítico", ResultadoAuditoria.EXITOSO);
            }
            cases.save(c);
        }
    }

    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void retryNotifications() {
        for (Notificacion n : notificationRepo.findTop100ByStatusOrderByCreatedAtAsc(EstadoNotificacion.REINTENTANDO)) {
            notifications.trySend(n);
            audit.logSystem("NOTIFICACIONES", "REINTENTO", "NOTIFICACION", n.getId().toString(), "Reintento automático de notificación", n.getStatus() == EstadoNotificacion.ENTREGADO ? ResultadoAuditoria.EXITOSO : ResultadoAuditoria.FALLIDO);
        }
    }
}
