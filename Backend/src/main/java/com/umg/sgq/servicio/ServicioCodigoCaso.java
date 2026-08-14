package com.umg.sgq.servicio;

import com.umg.sgq.entidad.SecuenciaCaso;
import com.umg.sgq.enumeracion.TipoCaso;
import com.umg.sgq.repositorio.RepositorioSecuenciaCaso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

@Service
public class ServicioCodigoCaso {
    private final RepositorioSecuenciaCaso sequences;

    public ServicioCodigoCaso(RepositorioSecuenciaCaso sequences) {
        this.sequences = sequences;
    }

    @Transactional
    public String next(TipoCaso type) {
        int year = Year.now().getValue();
        SecuenciaCaso seq = sequences.findForUpdate(type, year).orElseGet(() -> {
            SecuenciaCaso s = new SecuenciaCaso();
            s.setType(type);
            s.setYear(year);
            s.setNextValue(1);
            return sequences.saveAndFlush(s);
        });
        long value = seq.getNextValue();
        seq.setNextValue(value + 1);
        sequences.save(seq);
        String prefix;
        switch (type) {
            case QUEJA:
                prefix = "QUE";
                break;
            case RECLAMO:
                prefix = "REC";
                break;
            case DENUNCIA:
                prefix = "DEN";
                break;
            case SUGERENCIA:
                prefix = "SUG";
                break;
            default:
                throw new IllegalArgumentException("Tipo de caso no soportado: " + type);
        }
        return "%s-%d-%06d".formatted(prefix, year, value);
    }
}
