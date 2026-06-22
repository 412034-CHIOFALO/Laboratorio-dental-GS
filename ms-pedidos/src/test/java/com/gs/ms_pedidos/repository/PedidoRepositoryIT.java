package com.gs.ms_pedidos.repository;

import com.gs.ms_pedidos.model.EstadoPedido;
import com.gs.ms_pedidos.model.Pedido;
import com.gs.ms_pedidos.model.Prioridad;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PedidoRepositoryIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    PedidoRepository repo;

    private Pedido pedido(String nro, Long odontologoId, EstadoPedido estado) {
        return Pedido.builder()
                .nroPedido(nro)
                .odontologoId(odontologoId)
                .odontologoNombre("Dr. Test")
                .paciente("Paciente Test")
                .trabajo("Corona zirconio")
                .fechaEntrega(LocalDate.now().plusDays(7))
                .estado(estado)
                .prioridad(Prioridad.NORMAL)
                .build();
    }

    @Test
    void findByNroPedido_devuelvePedidoExistente() {
        repo.save(pedido("PED-001", 1L, EstadoPedido.RECIBIDO));

        Optional<Pedido> resultado = repo.findByNroPedido("PED-001");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getOdontologoId()).isEqualTo(1L);
    }

    @Test
    void findByNroPedido_devuelveVacioSiNoExiste() {
        assertThat(repo.findByNroPedido("NO-EXISTE")).isEmpty();
    }

    @Test
    void existsByNroPedido_trueParaExistente_falseParaInexistente() {
        repo.save(pedido("PED-002", 2L, EstadoPedido.RECIBIDO));

        assertThat(repo.existsByNroPedido("PED-002")).isTrue();
        assertThat(repo.existsByNroPedido("PED-999")).isFalse();
    }

    @Test
    void findByEstado_filtraCorrectamente() {
        repo.save(pedido("PED-003", 1L, EstadoPedido.RECIBIDO));
        repo.save(pedido("PED-004", 1L, EstadoPedido.EN_PROCESO));
        repo.save(pedido("PED-005", 2L, EstadoPedido.EN_PROCESO));

        List<Pedido> enProceso = repo.findByEstado(EstadoPedido.EN_PROCESO);
        List<Pedido> recibidos = repo.findByEstado(EstadoPedido.RECIBIDO);

        assertThat(enProceso).hasSize(2);
        assertThat(recibidos).hasSize(1);
    }

    @Test
    void ultimaActividadPorOdontologo_devuelveUnaFilaPorOdontologo() {
        repo.save(pedido("PED-006", 10L, EstadoPedido.RECIBIDO));
        repo.save(pedido("PED-007", 10L, EstadoPedido.ENTREGADO));
        repo.save(pedido("PED-008", 20L, EstadoPedido.RECIBIDO));

        List<Object[]> actividad = repo.ultimaActividadPorOdontologo();

        // Dos odontólogos distintos → dos filas
        assertThat(actividad).hasSize(2);
        // Cada fila: [odontologoId (Long), maxFechaCreacion (LocalDateTime)]
        assertThat(actividad).allSatisfy(row -> {
            assertThat(row).hasSize(2);
            assertThat(row[0]).isInstanceOf(Long.class);
        });
    }
}
