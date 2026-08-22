package com.umg.sgq.servicio;

import com.umg.sgq.entidad.*;
import com.umg.sgq.enumeracion.*;
import com.umg.sgq.repositorio.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ServicioNotificaciones {
    private final RepositorioNotificacion notifications;
    private final RepositorioPlantillaNotificacion templates;
    private final ObjectProvider<JavaMailSender> mailProvider;
    private final ServicioAuditoria audit;
    private final boolean mailEnabled;
    private final String from;

    public ServicioNotificaciones(RepositorioNotificacion notifications, RepositorioPlantillaNotificacion templates, ObjectProvider<JavaMailSender> mailProvider, ServicioAuditoria audit,
                               @Value("${app.mail.enabled:false}") boolean mailEnabled, @Value("${app.mail.from:no-reply@sgq.local}") String from) {
        this.notifications = notifications;
        this.templates = templates;
        this.mailProvider = mailProvider;
        this.audit = audit;
        this.mailEnabled = mailEnabled;
        this.from = from;
    }

    @Transactional
    public Notificacion email(EventoNotificacion event, Caso c, String recipient, String message) {
        Notificacion n = base(event, c, CanalNotificacion.CORREO, recipient, message, null);
        if (recipient == null || recipient.isBlank()) {
            n.setStatus(EstadoNotificacion.NO_APLICA);
            n.setLastError("Sin medio de contacto");
            return notifications.save(n);
        }
        notifications.save(n);
        return trySend(n);
    }

    @Transactional
    public Notificacion internal(EventoNotificacion event, Caso c, Usuario recipient, String message) {
        if (recipient == null) return null;
        Notificacion n = base(event, c, CanalNotificacion.INTERNA, recipient.getUsername(), message, recipient);
        n.setStatus(EstadoNotificacion.ENTREGADO);
        n.setSentAt(LocalDateTime.now());
        return notifications.save(n);
    }

    private Notificacion base(EventoNotificacion event, Caso c, CanalNotificacion channel, String recipient, String message, Usuario recipientUser) {
        Notificacion n = new Notificacion();
        n.setEvent(event);
        n.setComplaintCase(c);
        n.setChannel(channel);
        n.setRecipient(recipient);
        n.setRecipientUser(recipientUser);
        var t = templates.findByEventAndChannelAndActiveTrue(event, channel).orElse(null);
        String code = c == null ? "N/A" : c.getCode();
        String subject = t == null ? "SGQ - " + event.name() : render(t.getSubject(), event, code, message);
        String content = t == null ? message : render(t.getContent(), event, code, message);
        n.setSubject(subject);
        n.setContent(content);
        return n;
    }

    private String render(String s, EventoNotificacion e, String code, String message) {
        if (s == null) return null;
        return s.replace("{{evento}}", e.name()).replace("{{codigoCaso}}", code).replace("{{mensaje}}", message == null ? "" : message);
    }

    @Transactional
    public Notificacion trySend(Notificacion n) {

        n.setAttempts(n.getAttempts() + 1);

        /*
         * Si el correo está deshabilitado en desarrollo,
         * no fingimos que fue entregado realmente.
         */
        if (!mailEnabled) {

            n.setStatus(EstadoNotificacion.PENDIENTE);

            n.setLastError(
                    "Servicio de correo deshabilitado."
            );

            return notifications.save(n);
        }

        try {

            JavaMailSender sender =
                    mailProvider.getIfAvailable();

            if (sender == null) {

                throw new IllegalStateException(
                        "Proveedor de correo no configurado"
                );
            }

            SimpleMailMessage mensaje =
                    new SimpleMailMessage();

            mensaje.setFrom(from);

            mensaje.setTo(
                    n.getRecipient()
            );

            mensaje.setSubject(
                    n.getSubject()
            );

            mensaje.setText(
                    n.getContent()
            );

            sender.send(mensaje);

            n.setStatus(
                    EstadoNotificacion.ENTREGADO
            );

            n.setSentAt(
                    LocalDateTime.now()
            );

            n.setLastError(null);

        }catch (Exception e) {

            System.err.println("==========================================");
            System.err.println("ERROR ENVIANDO CORREO");
            System.err.println("Destinatario: " + n.getRecipient());
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.err.println("==========================================");

            n.setStatus(
                    n.getAttempts() < 3
                            ? EstadoNotificacion.REINTENTANDO
                            : EstadoNotificacion.FALLIDO
            );

            n.setLastError(
                    safe(e.getMessage())
            );
        }

        return notifications.save(n);
    }

    @Transactional
    public Notificacion resend(Long id, HttpServletRequest req, String reason) {
        Notificacion old = notifications.findById(id).orElseThrow(() -> new IllegalArgumentException("Notificación no encontrada."));
        if (old.getStatus() == EstadoNotificacion.NO_APLICA)
            throw new IllegalArgumentException("No aplica - sin medio de contacto.");
        if (old.getStatus() == EstadoNotificacion.ENTREGADO && (reason == null || reason.isBlank()))
            throw new IllegalArgumentException("Debe ingresar un motivo para reenviar una notificación entregada.");
        Notificacion n = new Notificacion();
        n.setOriginalNotification(old);
        n.setComplaintCase(old.getComplaintCase());
        n.setRecipientUser(old.getRecipientUser());
        n.setEvent(old.getEvent());
        n.setChannel(old.getChannel());
        n.setRecipient(old.getRecipient());
        n.setSubject(old.getSubject());
        n.setContent(old.getContent());
        notifications.save(n);
        n = old.getChannel() == CanalNotificacion.CORREO ? trySend(n) : markInternal(n);
        audit.log(req, "NOTIFICACIONES", "REENVIO", "NOTIFICACION", n.getId().toString(), "Reenvío manual de notificación", ResultadoAuditoria.EXITOSO, null, Map.of("motivo", reason == null ? "reintento" : reason));
        return n;
    }

    private Notificacion markInternal(Notificacion n) {
        n.setStatus(EstadoNotificacion.ENTREGADO);
        n.setSentAt(LocalDateTime.now());
        return notifications.save(n);
    }

    @Transactional
    public void cancel(Long id, String reason, HttpServletRequest req) {
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Debe ingresar un motivo.");
        Notificacion n = notifications.findById(id).orElseThrow();
        if (n.getStatus() != EstadoNotificacion.PENDIENTE && n.getStatus() != EstadoNotificacion.REINTENTANDO)
            throw new IllegalArgumentException("Solo se pueden cancelar notificaciones pendientes o en reintento.");
        n.setStatus(EstadoNotificacion.CANCELADO);
        n.setCancellationReason(reason);
        notifications.save(n);
        audit.log(req, "NOTIFICACIONES", "CANCELACION", "NOTIFICACION", id.toString(), "Notificación cancelada", ResultadoAuditoria.EXITOSO, null, Map.of("motivo", reason));
    }

    public List<Notificacion> history() {
        return notifications.findTop200ByOrderByCreatedAtDesc();
    }

    public List<Notificacion> inbox(Long userId) {
        return notifications.findByRecipientUserIdAndStatusNotOrderByCreatedAtDesc(userId, EstadoNotificacion.CANCELADO);
    }

    private String safe(String s) {
        if (s == null) return "Fallo de envío";
        return s.length() > 700 ? s.substring(0, 700) : s;
    }
}
