package com.umg.sgq.dto;

import com.umg.sgq.enumeracion.*;
import jakarta.validation.constraints.*;

import java.util.Set;

public final class DtosAdministracion {
    private DtosAdministracion() {
    }

    public record SolicitudUsuario(@NotBlank @Size(max = 120) String fullName,
                              @NotBlank @Size(min = 4, max = 30) String username, @NotBlank @Email String email,
                              @NotNull CodigoRol role, Long branchId, @NotNull EstadoRegistro status,
                              String temporaryPassword) {
    }

    public record RespuestaCreacionUsuario(VistaUsuario user, String temporaryPassword) {
    }

    public record VistaUsuario(Long id, String fullName, String username, String email, CodigoRol role, Long branchId,
                           String branch, EstadoRegistro status, int failedAttempts, String lockedUntil,
                           String lastLoginAt, boolean mustChangePassword, long version) {
    }

    public record VistaRol(Long id, CodigoRol code, String name, String description, boolean active,
                           boolean protectedRole, Set<CodigoPermiso> permissions) {
    }

    public record SolicitudPermisosRol(@NotNull Set<CodigoPermiso> permissions) {
    }

    public record SolicitudSucursal(@NotBlank @Size(min = 2, max = 10) String code,
                                @NotBlank @Size(min = 3, max = 100) String name,
                                @NotBlank @Size(max = 250) String address, @NotBlank @Size(max = 100) String department,
                                @NotBlank @Size(max = 100) String municipality,
                                @Size(max = 250) String locationReference,
                                @Pattern(regexp = "^$|\\d{8,15}$") String phone, @Email String email,
                                @Size(max = 120) String businessHours, @Size(max = 1000) String observations,
                                @NotNull EstadoRegistro status, Long supervisorId) {
    }

    public record VistaSucursal(Long id, String code, String name, String address, String department, String municipality,
                             String locationReference, String phone, String email, String businessHours,
                             String observations, EstadoRegistro status, Long supervisorId, String supervisor,
                             long activeUsers, long activeCases, String createdAt, String updatedAt, long version) {
    }
}
