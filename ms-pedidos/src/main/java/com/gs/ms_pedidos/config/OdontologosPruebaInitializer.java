package com.gs.ms_pedidos.config;

import com.gs.ms_pedidos.model.Odontologo;
import com.gs.ms_pedidos.repository.OdontologoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agrega un par de odontólogos "de práctica" para poder probar el flujo de
 * pedidos en producción sin usar clientes reales. Se ven como cualquier otro
 * odontólogo cargado (nombre y clínica realistas, sin "Prueba"/"Test" en el
 * nombre) para no ensuciar la vista con datos que se noten como ficticios.
 *
 * A diferencia de {@link DevDataInitializer} (solo dev, y solo corre si la
 * tabla está vacía), este corre en TODOS los ambientes y es idempotente por
 * nombre — no toca ni duplica los odontólogos reales ya cargados por uso
 * normal del sistema.
 *
 * <p>Sin DNI/CUIT (son opcionales) para no arriesgar un choque de unicidad
 * contra datos reales.</p>
 *
 * <p>Migración: la primera versión de este seed usaba "Dr. Prueba Uno" /
 * "Dra. Prueba Dos" — si ya están cargados con esos nombres, se actualizan
 * in-place a los nuevos datos en vez de crear duplicados.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OdontologosPruebaInitializer implements CommandLineRunner {

    private final OdontologoRepository repository;

    private record Datos(String nombreViejo, String nombre, String telefono, String email,
                          String matricula, String clinica) {}

    @Override
    public void run(String... args) {
        List<Datos> base = List.of(
            new Datos("Dr. Prueba Uno", "Dr. Roberto Fernández",
                "351-455-2210", "roberto.fernandez@odontologia.com.ar",
                "MP 51234", "Consultorio Fernández"),
            new Datos("Dra. Prueba Dos", "Dra. Valentina Torres",
                "351-478-3392", "valentina.torres@clinicadental.com.ar",
                "MP 52890", "Clínica Dental Torres")
        );

        int actualizados = 0;
        int creados = 0;

        for (Datos d : base) {
            var existenteNuevo = repository.findByActivoTrueAndNombreIgnoreCase(d.nombre());
            if (existenteNuevo.isPresent()) {
                continue; // ya está con el nombre definitivo, nada que hacer
            }

            var existenteViejo = repository.findByActivoTrueAndNombreIgnoreCase(d.nombreViejo());
            if (existenteViejo.isPresent()) {
                Odontologo o = existenteViejo.get();
                o.setNombre(d.nombre());
                o.setTelefono(d.telefono());
                o.setEmail(d.email());
                o.setMatricula(d.matricula());
                o.setClinica(d.clinica());
                repository.save(o);
                actualizados++;
                continue;
            }

            repository.save(Odontologo.builder()
                .nombre(d.nombre())
                .telefono(d.telefono())
                .email(d.email())
                .matricula(d.matricula())
                .clinica(d.clinica())
                .build());
            creados++;
        }

        if (actualizados == 0 && creados == 0) {
            log.info("[GS-PEDIDOS] Odontólogos de práctica ya presentes — nada que hacer.");
        } else {
            log.info("[GS-PEDIDOS] Odontólogos de práctica: {} actualizado(s), {} creado(s).", actualizados, creados);
        }
    }
}
