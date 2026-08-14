package com.umg.sgq.controlador;

import com.umg.sgq.servicio.ServicioPanel;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class ControladorPanel {
    private final ServicioPanel s;

    public ControladorPanel(ServicioPanel s) {
        this.s = s;
    }

    @GetMapping
    public Map<String, Object> summary() {
        return s.summary();
    }
}
