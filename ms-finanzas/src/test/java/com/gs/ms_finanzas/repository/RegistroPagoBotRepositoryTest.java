package com.gs.ms_finanzas.repository;

import com.gs.ms_finanzas.model.EstadoRegistroBot;
import com.gs.ms_finanzas.model.RegistroPagoBot;
import com.gs.ms_finanzas.model.TipoReceptorBot;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración (slice JPA con H2) del repositorio del historial del bot.
 * Verifica el anti-duplicado por nro de operación y el orden descendente.
 */
@DataJpaTest
class RegistroPagoBotRepositoryTest {

    @Autowired
    private RegistroPagoBotRepository repo;

    private RegistroPagoBot registro(String idOp, EstadoRegistroBot estado, LocalDateTime fecha) {
        return RegistroPagoBot.builder()
                .idOperacion(idOp)
                .monto(new BigDecimal("1000.00"))
                .estado(estado)
                .tipoReceptor(TipoReceptorBot.PROVEEDOR)
                .fechaHora(fecha)
                .build();
    }

    @Test
    void existsByIdOperacionAndEstado_detectaSoloLosRegistradosConEseIdOperacion() {
        repo.save(registro("OP-1", EstadoRegistroBot.REGISTRADO, LocalDateTime.now()));

        assertThat(repo.existsByIdOperacionAndEstado("OP-1", EstadoRegistroBot.REGISTRADO)).isTrue();
        assertThat(repo.existsByIdOperacionAndEstado("OP-2", EstadoRegistroBot.REGISTRADO)).isFalse();
        // mismo idOperacion pero otro estado → no es duplicado registrado
        assertThat(repo.existsByIdOperacionAndEstado("OP-1", EstadoRegistroBot.RECHAZADO)).isFalse();
    }

    @Test
    void findAllByOrderByFechaHoraDesc_devuelveDelMasNuevoAlMasViejo() {
        LocalDateTime ahora = LocalDateTime.now();
        repo.save(registro("VIEJO", EstadoRegistroBot.REGISTRADO, ahora.minusHours(2)));
        repo.save(registro("NUEVO", EstadoRegistroBot.REGISTRADO, ahora));

        List<RegistroPagoBot> orden = repo.findAllByOrderByFechaHoraDesc();

        assertThat(orden).hasSize(2);
        assertThat(orden.get(0).getIdOperacion()).isEqualTo("NUEVO");
        assertThat(orden.get(1).getIdOperacion()).isEqualTo("VIEJO");
    }
}
