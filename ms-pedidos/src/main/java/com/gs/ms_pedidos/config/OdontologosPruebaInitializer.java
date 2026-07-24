package com.gs.ms_pedidos.config;

import com.gs.ms_pedidos.model.Odontologo;
import com.gs.ms_pedidos.repository.OdontologoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agrega un par de odontólogos claramente marcados como "de prueba" para poder
 * probar el flujo de pedidos en producción sin usar clientes reales. A
 * diferencia de {@link DevDataInitializer} (solo dev, y además solo corre si
 * la tabla está vacía), este corre en TODOS los ambientes y es idempotente
 * por nombre — no toca ni duplica los odontólogos reales ya cargados por uso
 * normal del sistema.
 *
 * <p>Sin DNI/CUIT (son opcionales) para no arriesgar un choque de unicidad
 * contra datos reales. Se identifican como "de prueba" en el nombre y la
 * clínica para no confundirlos con clientes reales en los listados.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OdontologosPruebaInitializer implements CommandLineRunner {

    private final OdontologoRepository repository;

    @Override
    public void run(String... args) {
        List<Odontologo> base = List.of(
            Odontologo.builder()
                .nombre("Dr. Prueba Uno")
                .telefono("351-000-0001")
                .email("prueba1@laboratoriogs.test")
                .matricula("MP-TEST-001")
                .clinica("Consultorio de Prueba 1")
                .build(),
            Odontologo.builder()
                .nombre("Dra. Prueba Dos")
                .telefono("351-000-0002")
                .email("prueba2@laboratoriogs.test")
                .matricula("MP-TEST-002")
                .clinica("Consultorio de Prueba 2")
                .build()
        );

        List<Odontologo> faltantes = base.stream()
            .filter(o -> repository.findByActivoTrueAndNombreIgnoreCase(o.getNombre()).isEmpty())
            .toList();

        if (faltantes.isEmpty()) {
            log.info("[GS-PEDIDOS] Odontólogos de prueba ya presentes — nada que agregar.");
            return;
        }

        repository.saveAll(faltantes);
        log.info("[GS-PEDIDOS] {} odontólogo(s) de prueba agregado(s): {}",
            faltantes.size(), faltantes.stream().map(Odontologo::getNombre).toList());
    }
}
