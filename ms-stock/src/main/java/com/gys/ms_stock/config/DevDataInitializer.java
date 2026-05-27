package com.gys.ms_stock.config;

import com.gys.ms_stock.model.CategoriaMaterial;
import com.gys.ms_stock.model.Material;
import com.gys.ms_stock.repository.MaterialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Carga el inventario inicial de materiales de prueba.
 *
 * Incluye materiales con stock normal, uno bajo (alerta)
 * y uno agotado, para poder probar el sistema de alertas de stock.
 */
@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);
    private final MaterialRepository repository;

    public DevDataInitializer(MaterialRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("[GYS-DEV] ms-stock ya tiene datos — se omite la carga inicial.");
            return;
        }

        List<Material> materiales = List.of(

            // Stock normal
            Material.builder()
                .nombre("Cerámica Vita PM9")
                .descripcion("Cerámica feldespática de alta translucidez para coronas anteriores.")
                .categoria(CategoriaMaterial.CERAMICA)
                .stockActual(480.0)
                .stockMinimo(100.0)
                .unidadMedida("g")
                .precioUnitario(new BigDecimal("850.00"))
                .proveedor("Dental Import SRL")
                .activo(true)
                .build(),

            // Stock normal
            Material.builder()
                .nombre("Resina Bis-GMA Esthet-X")
                .descripcion("Resina de nano-relleno para incrustaciones directas e indirectas.")
                .categoria(CategoriaMaterial.RESINA)
                .stockActual(180.0)
                .stockMinimo(50.0)
                .unidadMedida("ml")
                .precioUnitario(new BigDecimal("1200.00"))
                .proveedor("Dentsply Sirona Argentina")
                .activo(true)
                .build(),

            // Stock normal
            Material.builder()
                .nombre("Yeso Dentona Tipo IV")
                .descripcion("Yeso piedra extraduro para modelos de trabajo. Expansión controlada.")
                .categoria(CategoriaMaterial.YESO)
                .stockActual(4200.0)
                .stockMinimo(1000.0)
                .unidadMedida("g")
                .precioUnitario(new BigDecimal("25.00"))
                .proveedor("Laboratorios Lascod")
                .activo(true)
                .build(),

            // ⚠ BAJO STOCK — activará alerta en el dashboard
            Material.builder()
                .nombre("Cera Inlay Modeling")
                .descripcion("Cera de alta consistencia para modelado de coronas e incrustaciones.")
                .categoria(CategoriaMaterial.CERA)
                .stockActual(35.0)   // ← por debajo del mínimo (50g)
                .stockMinimo(50.0)
                .unidadMedida("g")
                .precioUnitario(new BigDecimal("120.00"))
                .proveedor("Renfert GmbH")
                .activo(true)
                .build(),

            // Stock normal
            Material.builder()
                .nombre("Fresas de Carburo Tungsteno")
                .descripcion("Set de fresas para tallado y ajuste de restauraciones fijas.")
                .categoria(CategoriaMaterial.HERRAMIENTA)
                .stockActual(18.0)
                .stockMinimo(5.0)
                .unidadMedida("unidad")
                .precioUnitario(new BigDecimal("800.00"))
                .proveedor("Komet Dental")
                .activo(true)
                .build(),

            // ⚠ AGOTADO — para testear el caso extremo
            Material.builder()
                .nombre("Acrílico Termoplástico QC-20")
                .descripcion("Acrílico de polimerización en agua caliente para aparatología removible.")
                .categoria(CategoriaMaterial.RESINA)
                .stockActual(0.0)    // ← agotado
                .stockMinimo(200.0)
                .unidadMedida("g")
                .precioUnitario(new BigDecimal("450.00"))
                .proveedor("Dentsply Sirona Argentina")
                .activo(true)
                .build()
        );

        repository.saveAll(materiales);
        log.info("[GYS-DEV] {} materiales cargados en stock.", materiales.size());
        log.info("[GYS-DEV] Alertas de bajo stock: Cera Inlay (35g < 50g min), Acrílico QC-20 (0g)");
    }
}
