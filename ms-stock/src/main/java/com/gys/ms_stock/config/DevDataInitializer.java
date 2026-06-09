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
 * Carga el inventario inicial — mismos materiales que los mocks del frontend,
 * en el mismo orden de inserción para que los IDs coincidan.
 *
 * Mix intencional:
 *   - Materiales medibles (descuentaStock=true): yeso, aleación, acrílico, etc.
 *   - Materiales "por uso" (descuentaStock=false): cerámicas y porcelanas (se
 *     aplican con pincel, no tiene sentido descontar gramos exactos).
 *   - Algunos en bajo stock para que se vea la alerta.
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

            // 1 — Yeso Piedra Tipo IV — bajo stock
            Material.builder()
                .nombre("Yeso Piedra Tipo IV")
                .descripcion("Yeso tipo IV para modelos de trabajo. Expansión controlada.")
                .categoria(CategoriaMaterial.YESO)
                .stockActual(3.0).stockMinimo(5.0).unidadMedida("pote")
                .precioUnitario(new BigDecimal("3500.00"))
                .proveedor("Casa Dental Norte")
                .descuentaStock(true)
                .activo(true)
                .build(),

            // 2 — Cerámica Vita VM13 — bajo stock + "por uso" (no descuenta)
            Material.builder()
                .nombre("Cerámica Vita VM13")
                .descripcion("Cerámica feldespática para metal-cerámica. Se aplica con pincel.")
                .categoria(CategoriaMaterial.CERAMICA)
                .stockActual(2.0).stockMinimo(4.0).unidadMedida("frasco")
                .precioUnitario(new BigDecimal("8500.00"))
                .proveedor("Dental Supply SRL")
                .descuentaStock(false)   // ← se usa con pincel, solo se verifica
                .activo(true)
                .build(),

            // 3 — Aleación Cr-Co
            Material.builder()
                .nombre("Aleación Cr-Co NPG")
                .descripcion("Aleación no precious para metal-cerámica. Medible al gramo.")
                .categoria(CategoriaMaterial.METAL)
                .stockActual(450.0).stockMinimo(200.0).unidadMedida("gramo")
                .precioUnitario(new BigDecimal("95.00"))
                .proveedor("MetalDent SA")
                .descuentaStock(true)
                .activo(true)
                .build(),

            // 4 — Acrílico Rosa Termocurable
            Material.builder()
                .nombre("Acrílico Rosa Termocurable")
                .descripcion("Acrílico termocurable para prótesis removibles.")
                .categoria(CategoriaMaterial.ACRILICO)
                .stockActual(12.0).stockMinimo(6.0).unidadMedida("frasco")
                .precioUnitario(new BigDecimal("4200.00"))
                .proveedor("Casa Dental Norte")
                .descuentaStock(true)
                .activo(true)
                .build(),

            // 5 — Alambre Inox
            Material.builder()
                .nombre("Alambre Inox 0.7mm")
                .descripcion("Alambre de acero inoxidable para ortodoncia.")
                .categoria(CategoriaMaterial.ALAMBRE)
                .stockActual(8.0).stockMinimo(3.0).unidadMedida("rollo")
                .precioUnitario(new BigDecimal("1800.00"))
                .proveedor("Dental Supply SRL")
                .descuentaStock(true)
                .activo(true)
                .build(),

            // 6 — Resina Autopolimerizable — bajo stock
            Material.builder()
                .nombre("Resina Autopolimerizable")
                .descripcion("Para férulas y provisorios. Polímero + monómero.")
                .categoria(CategoriaMaterial.RESINA)
                .stockActual(4.0).stockMinimo(4.0).unidadMedida("kit")
                .precioUnitario(new BigDecimal("6200.00"))
                .proveedor("Casa Dental Norte")
                .descuentaStock(true)
                .activo(true)
                .build(),

            // 7 — Discos Zirconia
            Material.builder()
                .nombre("Discos Zirconia 98mm")
                .descripcion("Para coronas de zirconio monolítico. Se cuenta por unidad.")
                .categoria(CategoriaMaterial.ZIRCONIA)
                .stockActual(6.0).stockMinimo(3.0).unidadMedida("unidad")
                .precioUnitario(new BigDecimal("18500.00"))
                .proveedor("Zirkon Dental")
                .descuentaStock(true)
                .activo(true)
                .build(),

            // 8 — Porcelana Vita VMK — "por uso" (no descuenta)
            Material.builder()
                .nombre("Porcelana Vita VMK")
                .descripcion("Porcelana de cocción para coronas. Se aplica con pincel.")
                .categoria(CategoriaMaterial.PORCELANA)
                .stockActual(5.0).stockMinimo(3.0).unidadMedida("frasco")
                .precioUnitario(new BigDecimal("7800.00"))
                .proveedor("Dental Supply SRL")
                .descuentaStock(false)   // ← se usa con pincel
                .activo(true)
                .build(),

            // 9 — Separadores de Goma — "por uso"
            Material.builder()
                .nombre("Separadores de Goma")
                .descripcion("Para separar dientes en yeso. Reutilizables.")
                .categoria(CategoriaMaterial.CONSUMIBLE)
                .stockActual(15.0).stockMinimo(5.0).unidadMedida("bolsa")
                .precioUnitario(new BigDecimal("1200.00"))
                .proveedor("Casa Dental Norte")
                .descuentaStock(false)   // ← se cuenta visualmente
                .activo(true)
                .build()
        );

        repository.saveAll(materiales);
        long medibles = materiales.stream().filter(Material::isDescuentaStock).count();
        log.info("[GYS-DEV] {} materiales cargados en stock ({} medibles + {} por uso).",
            materiales.size(), medibles, materiales.size() - medibles);
        log.info("[GYS-DEV] Alertas: Yeso Tipo IV (3 < 5), Cerámica VM13 (2 < 4), Resina Auto (4 = 4)");
    }
}
