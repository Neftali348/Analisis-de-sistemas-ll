package com.umg.sgq.controlador;

import com.umg.sgq.dto.DtosAutenticacion.*;
import com.umg.sgq.servicio.ServicioAutenticacion;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ControladorAutenticacion {
    private final ServicioAutenticacion auth;

    public ControladorAutenticacion(ServicioAutenticacion auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public RespuestaInicioSesion login(@Valid @RequestBody SolicitudInicioSesion r, HttpServletRequest req) {
        return auth.login(r, req);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest req) {
        auth.logout(req);
        return Map.of("message", "Sesión cerrada correctamente.");
    }
    @PostMapping("/expire")
    public Map<String, String> expire(
            HttpServletRequest req
    ) {

        auth.expirarSesion(req);

        return Map.of(
                "message",
                "Sesión expirada por inactividad."
        );
    }

    @PostMapping("/touch")
    public Map<String, String> touch() {
        return Map.of("message", "Sesión activa.");
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgot(
            @Valid
            @RequestBody
            SolicitudRecuperarContrasena r,
            HttpServletRequest req
    ) {

        return Map.of(
                "message",
                auth.forgotPassword(r, req)
        );
    }

    @PostMapping("/access-denied")
    public Map<String, String> accesoDenegado(
            @RequestBody Map<String, String> datos,
            HttpServletRequest req
    ) {

        auth.registrarAccesoDenegado(
                datos.get("permission"),
                datos.get("route"),
                req
        );

        return Map.of(
                "message",
                "Intento registrado."
        );
    }

    @PostMapping("/reset-password")
    public Map<String, String> reset(@Valid @RequestBody SolicitudRestablecerContrasena r, HttpServletRequest req) {
        auth.resetPassword(r, req);
        return Map.of("message", "Contraseña restablecida correctamente.");
    }

    @PostMapping("/change-password")
    public Map<String, String> change(@Valid @RequestBody SolicitudCambiarContrasena r, HttpServletRequest req) {
        auth.changePassword(r, req);
        return Map.of("message", "Contraseña actualizada correctamente.");
    }
}
