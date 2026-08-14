package com.umg.sgq;

import com.umg.sgq.enumeracion.*;
import com.umg.sgq.servicio.ServicioReglasCaso;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class PruebasReglasNegocio {
    private final ServicioReglasCaso rules = new ServicioReglasCaso();

    @Test
    void rn01DebeTenerExactamenteCuatroRolesInternos() {
        assertArrayEquals(
                new CodigoRol[]{CodigoRol.AGENTE_ATENCION, CodigoRol.SUPERVISOR, CodigoRol.ADMINISTRADOR, CodigoRol.AUDITOR},
                CodigoRol.values()
        );
        assertEquals(4, CodigoRol.values().length);
    }

    @Test
    void rn04SlaPorPrioridad() {
        assertEquals(Duration.ofHours(72), rules.sla(Prioridad.BAJA));
        assertEquals(Duration.ofHours(48), rules.sla(Prioridad.MEDIA));
        assertEquals(Duration.ofHours(24), rules.sla(Prioridad.ALTA));
        assertEquals(Duration.ofHours(4), rules.sla(Prioridad.CRITICA));
    }

    @Test
    void rn10ValidaTransiciones() {
        assertDoesNotThrow(() -> rules.requireTransition(EstadoCaso.REGISTRADO, EstadoCaso.PENDIENTE_ASIGNACION));
        assertDoesNotThrow(() -> rules.requireTransition(EstadoCaso.RESUELTO, EstadoCaso.CERRADO));
        assertThrows(IllegalArgumentException.class, () -> rules.requireTransition(EstadoCaso.REGISTRADO, EstadoCaso.CERRADO));
        assertThrows(IllegalArgumentException.class, () -> rules.requireTransition(EstadoCaso.CERRADO, EstadoCaso.EN_PROCESO));
    }
}
