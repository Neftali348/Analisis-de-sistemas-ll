package com.umg.sgq.entidad;

import com.umg.sgq.enumeracion.TipoCaso;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "case_sequences", uniqueConstraints = @UniqueConstraint(name = "uk_case_seq", columnNames = {"type", "year_value"}))
@Getter
@Setter
public class SecuenciaCaso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCaso type;
    @Column(name = "year_value", nullable = false)
    private int year;
    @Column(nullable = false)
    private long nextValue = 1;
}
