package com.umg.sgq.entidad;

import com.umg.sgq.enumeracion.CodigoPermiso;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "permissions")
@Getter
@Setter
public class Permiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 60)
    private CodigoPermiso code;
    @Column(nullable = false, length = 120)
    private String description;
}
