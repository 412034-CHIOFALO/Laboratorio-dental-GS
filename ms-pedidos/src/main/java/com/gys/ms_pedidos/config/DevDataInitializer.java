package com.gys.ms_pedidos.config;

import com.gys.ms_pedidos.model.EstadoPedido;
import com.gys.ms_pedidos.model.Pedido;
import com.gys.ms_pedidos.model.Prioridad;
import com.gys.ms_pedidos.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Carga 5 pedidos de prueba que cubren todos los estados del flujo:
 *   RECIBIDO → EN_PROCESO → CONTROL → LISTO
 *
 * IDs de odontólogos (ms-auth DevDataInitializer, mismo orden de creación):
 *   ID 3 = dr_garcia   (Martín García)
 *   ID 4 = dra_sanchez (Laura Sánchez)
 *
 * ID de técnico:
 *   ID 2 = tecnico1 (Carlos López)
 */
@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);
    private final PedidoRepository repository;

    public DevDataInitializer(PedidoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("[GYS-DEV] ms-pedidos ya tiene datos — se omite la carga inicial.");
            return;
        }

        LocalDate hoy = LocalDate.now();

        List<Pedido> pedidos = List.of(

            // 1. RECIBIDO + URGENTE — corona recién ingresada, pendiente de asignar
            Pedido.builder()
                .nroPedido("GYS-2025-0001")
                .odontologoId(3L)
                .odontologoNombre("Dr. Martín García")
                .paciente("Martín López")
                .catalogoTrabajoId(1L)
                .trabajo("Corona Metal-Cerámica")
                .fechaEntrega(hoy.plusDays(5))
                .estado(EstadoPedido.RECIBIDO)
                .prioridad(Prioridad.URGENTE)
                .precioAcordado(new BigDecimal("15000.00"))
                .observaciones("Urgente — paciente con cita el " + hoy.plusDays(5))
                .build(),

            // 2. EN_PROCESO — prótesis asignada a Carlos
            Pedido.builder()
                .nroPedido("GYS-2025-0002")
                .odontologoId(3L)
                .odontologoNombre("Dr. Martín García")
                .paciente("Ana Rodríguez")
                .catalogoTrabajoId(4L)
                .trabajo("Prótesis Total Superior")
                .tecnicoId(2L)
                .tecnicoNombre("Carlos López")
                .fechaEntrega(hoy.plusDays(12))
                .estado(EstadoPedido.EN_PROCESO)
                .prioridad(Prioridad.NORMAL)
                .precioAcordado(new BigDecimal("45000.00"))
                .observaciones("Primera prótesis del paciente. Incluir ajuste de mordida.")
                .build(),

            // 3. EN_PROCESO — incrustación en proceso
            Pedido.builder()
                .nroPedido("GYS-2025-0003")
                .odontologoId(4L)
                .odontologoNombre("Dra. Laura Sánchez")
                .paciente("Luis Fernández")
                .catalogoTrabajoId(3L)
                .trabajo("Incrustación Onlay")
                .tecnicoId(2L)
                .tecnicoNombre("Carlos López")
                .fechaEntrega(hoy.plusDays(3))
                .estado(EstadoPedido.EN_PROCESO)
                .prioridad(Prioridad.NORMAL)
                .precioAcordado(new BigDecimal("8000.00"))
                .build(),

            // 4. CONTROL — aparato funcional listo para revisión
            Pedido.builder()
                .nroPedido("GYS-2025-0004")
                .odontologoId(4L)
                .odontologoNombre("Dra. Laura Sánchez")
                .paciente("Elena Gómez")
                .catalogoTrabajoId(5L)
                .trabajo("Aparato Funcional Bimler")
                .tecnicoId(2L)
                .tecnicoNombre("Carlos López")
                .fechaEntrega(hoy.plusDays(1))
                .estado(EstadoPedido.CONTROL)
                .prioridad(Prioridad.NORMAL)
                .precioAcordado(new BigDecimal("18000.00"))
                .observaciones("Verificar que el alambre labial quede libre del canino.")
                .build(),

            // 5. LISTO — férula entregada, queda facturación
            Pedido.builder()
                .nroPedido("GYS-2025-0005")
                .odontologoId(3L)
                .odontologoNombre("Dr. Martín García")
                .paciente("Roberto Díaz")
                .catalogoTrabajoId(6L)
                .trabajo("Férula Miorelajante ATM")
                .tecnicoId(2L)
                .tecnicoNombre("Carlos López")
                .fechaEntrega(hoy.minusDays(1))
                .estado(EstadoPedido.LISTO)
                .prioridad(Prioridad.NORMAL)
                .precioAcordado(new BigDecimal("12000.00"))
                .observaciones("Pulido final aprobado. Listo para retiro.")
                .build()
        );

        repository.saveAll(pedidos);
        log.info("[GYS-DEV] {} pedidos de prueba cargados en ms-pedidos.", pedidos.size());
        log.info("[GYS-DEV] Estados: RECIBIDO(1) EN_PROCESO(2) CONTROL(1) LISTO(1)");
    }
}
