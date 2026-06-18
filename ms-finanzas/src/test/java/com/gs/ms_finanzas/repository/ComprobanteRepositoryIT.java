package com.gs.ms_finanzas.repository;

import com.gs.ms_finanzas.model.Comprobante;
import com.gs.ms_finanzas.model.EstadoPago;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ComprobanteRepositoryIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    ComprobanteRepository repo;

    private Comprobante comprobante(String nro, Long odontologoId, BigDecimal monto, EstadoPago estado) {
        return Comprobante.builder()
                .nroComprobante(nro)
                .pedidoId(1L)
                .nroPedido("PED-REF")
                .odontologoId(odontologoId)
                .odontologoNombre("Dr. Moroso")
                .trabajo("Corona")
                .monto(monto)
                .estadoPago(estado)
                .fechaEmision(LocalDate.now())
                .build();
    }

    @Test
    void sumMontosPendientes_devuelveCeroCuandoNoHayRegistros() {
        BigDecimal resultado = repo.sumMontosPendientesByOdontologo(99L);

        // COALESCE garantiza 0 en vez de null
        assertThat(resultado).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void sumMontosPendientes_sumaUnicamentePendientes() {
        repo.save(comprobante("COMP-001", 1L, new BigDecimal("1500.00"), EstadoPago.PENDIENTE));
        repo.save(comprobante("COMP-002", 1L, new BigDecimal("2000.00"), EstadoPago.PENDIENTE));
        repo.save(comprobante("COMP-003", 1L, new BigDecimal("500.00"), EstadoPago.COBRADO));

        BigDecimal resultado = repo.sumMontosPendientesByOdontologo(1L);

        // El COBRADO no entra en la suma
        assertThat(resultado).isEqualByComparingTo(new BigDecimal("3500.00"));
    }

    @Test
    void rankingDeudoresRaw_ordenaDesMayorAMenorDeuda() {
        // Odontólogo 1: $3000 pendiente
        repo.save(comprobante("COMP-004", 1L, new BigDecimal("3000.00"), EstadoPago.PENDIENTE));
        // Odontólogo 2: $5000 pendiente (dos comprobantes)
        repo.save(comprobante("COMP-005", 2L, new BigDecimal("2000.00"), EstadoPago.PENDIENTE));
        repo.save(comprobante("COMP-006", 2L, new BigDecimal("3000.00"), EstadoPago.PENDIENTE));
        // Odontólogo 3: solo cobrado → no debe aparecer en el ranking
        repo.save(comprobante("COMP-007", 3L, new BigDecimal("1000.00"), EstadoPago.COBRADO));

        List<Object[]> ranking = repo.rankingDeudoresRaw();

        assertThat(ranking).hasSize(2);
        // Primera posición: odontólogo 2 con $5000 (mayor deuda)
        assertThat((Long) ranking.get(0)[0]).isEqualTo(2L);
        assertThat((BigDecimal) ranking.get(0)[2]).isEqualByComparingTo(new BigDecimal("5000.00"));
        // Segunda posición: odontólogo 1 con $3000
        assertThat((Long) ranking.get(1)[0]).isEqualTo(1L);
        assertThat((BigDecimal) ranking.get(1)[2]).isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    void rankingDeudoresRaw_devuelveVacioSinDeudas() {
        repo.save(comprobante("COMP-008", 5L, new BigDecimal("999.00"), EstadoPago.COBRADO));

        assertThat(repo.rankingDeudoresRaw()).isEmpty();
    }
}
