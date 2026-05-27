package com.gys.ms_produccion.config;

import com.gys.ms_produccion.model.EstadoTarea;
import com.gys.ms_produccion.model.Prioridad;
import com.gys.ms_produccion.model.TareaProduccion;
import com.gys.ms_produccion.repository.TareaProduccionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProduccionDataInitializer implements CommandLineRunner {

    private final TareaProduccionRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("Produccion ya inicializada - omitiendo seed ({} tareas)", repository.count());
            return;
        }

        log.info("Inicializando tareas de produccion con datos de ejemplo...");

        LocalDate hoy = LocalDate.now();

        List<TareaProduccion> tareas = List.of(

            // RECIBIDO
            TareaProduccion.builder()
                .nroPedido("PED-20260516-0001")
                .paciente("Garcia, Maria")
                .odontologoNombre("Dr. Fernandez")
                .trabajo("Corona de Zirconio")
                .tecnicoNombre("Carlos Rodriguez")
                .estado(EstadoTarea.RECIBIDO)
                .prioridad(Prioridad.URGENTE)
                .fechaIngreso(hoy.minusDays(1))
                .fechaEntregaEstimada(hoy.plusDays(3))
                .observaciones("Shade A2, verificar con el odontólogo antes de terminar")
                .build(),

            TareaProduccion.builder()
                .nroPedido("PED-20260517-0002")
                .paciente("Lopez, Juan Carlos")
                .odontologoNombre("Dra. Martinez")
                .trabajo("Prótesis Parcial Metálica")
                .tecnicoNombre(null)
                .estado(EstadoTarea.RECIBIDO)
                .prioridad(Prioridad.NORMAL)
                .fechaIngreso(hoy)
                .fechaEntregaEstimada(hoy.plusDays(12))
                .observaciones(null)
                .build(),

            TareaProduccion.builder()
                .nroPedido("PED-20260515-0003")
                .paciente("Perez, Ana Lucia")
                .odontologoNombre("Dr. Gomez")
                .trabajo("Puente de Porcelana (3 piezas)")
                .tecnicoNombre("Laura Sanchez")
                .estado(EstadoTarea.RECIBIDO)
                .prioridad(Prioridad.NORMAL)
                .fechaIngreso(hoy.minusDays(2))
                .fechaEntregaEstimada(hoy.plusDays(8))
                .observaciones("Paciente sensible, usar cementos de baja temperatura")
                .build(),

            // EN_PROCESO
            TareaProduccion.builder()
                .nroPedido("PED-20260512-0004")
                .paciente("Diaz, Roberto")
                .odontologoNombre("Dra. Martinez")
                .trabajo("Placa Miorelajante")
                .tecnicoNombre("Carlos Rodriguez")
                .estado(EstadoTarea.EN_PROCESO)
                .prioridad(Prioridad.NORMAL)
                .fechaIngreso(hoy.minusDays(5))
                .fechaEntregaEstimada(hoy.plusDays(2))
                .observaciones(null)
                .build(),

            TareaProduccion.builder()
                .nroPedido("PED-20260511-0005")
                .paciente("Torres, Claudia")
                .odontologoNombre("Dr. Fernandez")
                .trabajo("Carilla de Porcelana")
                .tecnicoNombre("Laura Sanchez")
                .estado(EstadoTarea.EN_PROCESO)
                .prioridad(Prioridad.URGENTE)
                .fechaIngreso(hoy.minusDays(6))
                .fechaEntregaEstimada(hoy.plusDays(1))
                .observaciones("Boda el sabado - URGENTE")
                .build(),

            TareaProduccion.builder()
                .nroPedido("PED-20260510-0006")
                .paciente("Ruiz, Miguel Angel")
                .odontologoNombre("Dr. Gomez")
                .trabajo("Prótesis Completa Superior")
                .tecnicoNombre("Pedro Alvarez")
                .estado(EstadoTarea.EN_PROCESO)
                .prioridad(Prioridad.NORMAL)
                .fechaIngreso(hoy.minusDays(7))
                .fechaEntregaEstimada(hoy.plusDays(7))
                .observaciones("Relacion intermaxilar tomada el martes")
                .build(),

            // CONTROL
            TareaProduccion.builder()
                .nroPedido("PED-20260508-0007")
                .paciente("Morales, Patricia")
                .odontologoNombre("Dra. Martinez")
                .trabajo("Retenedor de Hawley")
                .tecnicoNombre("Carlos Rodriguez")
                .estado(EstadoTarea.CONTROL)
                .prioridad(Prioridad.NORMAL)
                .fechaIngreso(hoy.minusDays(9))
                .fechaEntregaEstimada(hoy.plusDays(1))
                .observaciones("Verificar ajuste del arco vestibular")
                .build(),

            TareaProduccion.builder()
                .nroPedido("PED-20260507-0008")
                .paciente("Vega, Santiago")
                .odontologoNombre("Dr. Fernandez")
                .trabajo("Corona de Porcelana sobre Metal")
                .tecnicoNombre("Pedro Alvarez")
                .estado(EstadoTarea.CONTROL)
                .prioridad(Prioridad.URGENTE)
                .fechaIngreso(hoy.minusDays(10))
                .fechaEntregaEstimada(hoy)
                .observaciones("Entrega hoy - revision final")
                .build(),

            // LISTO
            TareaProduccion.builder()
                .nroPedido("PED-20260505-0009")
                .paciente("Jimenez, Elena")
                .odontologoNombre("Dr. Gomez")
                .trabajo("Aparato de Expansion Palatina")
                .tecnicoNombre("Laura Sanchez")
                .estado(EstadoTarea.LISTO)
                .prioridad(Prioridad.NORMAL)
                .fechaIngreso(hoy.minusDays(12))
                .fechaEntregaEstimada(hoy.minusDays(1))
                .observaciones("Listo para retirar")
                .build(),

            TareaProduccion.builder()
                .nroPedido("PED-20260504-0010")
                .paciente("Castro, Fernando")
                .odontologoNombre("Dra. Martinez")
                .trabajo("Perno Munon Colado")
                .tecnicoNombre("Carlos Rodriguez")
                .estado(EstadoTarea.LISTO)
                .prioridad(Prioridad.NORMAL)
                .fechaIngreso(hoy.minusDays(13))
                .fechaEntregaEstimada(hoy.minusDays(2))
                .observaciones(null)
                .build()
        );

        repository.saveAll(tareas);
        log.info("Produccion inicializada con {} tareas de ejemplo.", tareas.size());
    }
}
