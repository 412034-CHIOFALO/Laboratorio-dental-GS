package com.gys.ms_produccion.config;

import com.gys.ms_produccion.model.EstadoTarea;
import com.gys.ms_produccion.model.Prioridad;
import com.gys.ms_produccion.model.TareaProduccion;
import com.gys.ms_produccion.repository.TareaProduccionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Carga las tareas kanban correspondientes a los pedidos de DevDataInitializer
 * en ms-pedidos. Refleja el mismo estado para que ambos MS estén sincronizados.
 *
 * Columnas del Kanban: RECIBIDO | EN_PROCESO | CONTROL | LISTO
 */
@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);
    private final TareaProduccionRepository repository;

    public DevDataInitializer(TareaProduccionRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("[GYS-DEV] ms-produccion ya tiene datos — se omite la carga inicial.");
            return;
        }

        LocalDate hoy = LocalDate.now();

        List<TareaProduccion> tareas = List.of(

            // Columna RECIBIDO — corona urgente sin técnico asignado
            TareaProduccion.builder()
                .nroPedido("GYS-2025-0001")
                .paciente("Martín López")
                .odontologoNombre("Dr. Martín García")
                .trabajo("Corona Metal-Cerámica")
                .estado(EstadoTarea.RECIBIDO)
                .prioridad(Prioridad.URGENTE)
                .fechaIngreso(hoy)
                .fechaEntregaEstimada(hoy.plusDays(5))
                .observaciones("URGENTE — sin técnico asignado")
                .activo(true)
                .build(),

            // Columna EN_PROCESO — prótesis con Carlos
            TareaProduccion.builder()
                .nroPedido("GYS-2025-0002")
                .paciente("Ana Rodríguez")
                .odontologoNombre("Dr. Martín García")
                .trabajo("Prótesis Total Superior")
                .tecnicoNombre("Carlos López")
                .estado(EstadoTarea.EN_PROCESO)
                .prioridad(Prioridad.NORMAL)
                .fechaIngreso(hoy.minusDays(3))
                .fechaEntregaEstimada(hoy.plusDays(12))
                .observaciones("Etapa: prueba en cera completada. Falta cocción porcelana.")
                .activo(true)
                .build(),

            // Columna EN_PROCESO — incrustación con Carlos
            TareaProduccion.builder()
                .nroPedido("GYS-2025-0003")
                .paciente("Luis Fernández")
                .odontologoNombre("Dra. Laura Sánchez")
                .trabajo("Incrustación Onlay")
                .tecnicoNombre("Carlos López")
                .estado(EstadoTarea.EN_PROCESO)
                .prioridad(Prioridad.NORMAL)
                .fechaIngreso(hoy.minusDays(1))
                .fechaEntregaEstimada(hoy.plusDays(3))
                .activo(true)
                .build(),

            // Columna CONTROL — aparato funcional listo para revisión
            TareaProduccion.builder()
                .nroPedido("GYS-2025-0004")
                .paciente("Elena Gómez")
                .odontologoNombre("Dra. Laura Sánchez")
                .trabajo("Aparato Funcional Bimler")
                .tecnicoNombre("Carlos López")
                .estado(EstadoTarea.CONTROL)
                .prioridad(Prioridad.NORMAL)
                .fechaIngreso(hoy.minusDays(8))
                .fechaEntregaEstimada(hoy.plusDays(1))
                .observaciones("Pendiente verificación alambre labial y pulido final.")
                .activo(true)
                .build(),

            // Columna LISTO — férula entregada
            TareaProduccion.builder()
                .nroPedido("GYS-2025-0005")
                .paciente("Roberto Díaz")
                .odontologoNombre("Dr. Martín García")
                .trabajo("Férula Miorelajante ATM")
                .tecnicoNombre("Carlos López")
                .estado(EstadoTarea.LISTO)
                .prioridad(Prioridad.NORMAL)
                .fechaIngreso(hoy.minusDays(7))
                .fechaEntregaEstimada(hoy.minusDays(1))
                .observaciones("Entregado al odontólogo el " + hoy.minusDays(1))
                .activo(false) // ya finalizado, fuera del kanban activo
                .build()
        );

        repository.saveAll(tareas);
        log.info("[GYS-DEV] {} tareas de producción cargadas.", tareas.size());
        log.info("[GYS-DEV] Kanban: RECIBIDO(1) EN_PROCESO(2) CONTROL(1) LISTO(1)");
    }
}
