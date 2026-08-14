package com.umg.sgq.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public final class DtosAutenticacion {
    private DtosAutenticacion() {
    }

    public record SolicitudInicioSesion(@NotBlank(message = "Usuario obligatorio") String username,
                               @NotBlank(message = "Contraseña obligatoria") String password) {
    }

    public record RespuestaInicioSesion(String token, Long userId, String fullName, String username, String role, Long branchId,
                                Set<String> permissions, boolean mustChangePassword, String message) {
    }

    public record SolicitudRecuperarContrasena(@NotBlank String usernameOrEmail) {
    }

    public record SolicitudCambiarContrasena(@NotBlank String currentPassword, @NotBlank String newPassword,
                                        @NotBlank String confirmPassword) {
    }

    public record SolicitudRestablecerContrasena(@NotBlank String token, @NotBlank String newPassword,
                                       @NotBlank String confirmPassword) {
    }
}
