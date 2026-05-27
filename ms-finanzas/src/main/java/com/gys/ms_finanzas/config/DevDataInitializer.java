package com.gys.ms_finanzas.config;

import com.gys.ms_finanzas.model.*;
import com.gys.ms_finanzas.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Carga comprobantes de prueba para los pedidos en estado CONTROL y LISTO.
 *
 * Comprobantes:
 *   COMP-2025-001 — Aparato Funcional (pedido 0004) — $18.000 — PENDIENTE
 *   COMP-2025-002 — Férula ATM (pedido 0005)        — $12.000 — COBRADO
 *   COMP-2025-003 — Corona Metal-Cerámica (0001)     — $15.000 — VENCIDO (para ver alertas)
 */
@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);
    private final ComprobanteRepository repository;
    private final ProveedorRepository proveedorRepository;
    private final DeudaProveedorRepository deudaProveedorRepository;
    private final SueldoEmpleadoRepository sueldoEmpleadoRepository;

    public DevDataInitializer(ComprobanteRepository repository,
                              ProveedorRepository proveedorRepository,
                              DeudaProveedorRepository deudaProveedorRepository,
                              SueldoEmpleadoRepository sueldoEmpleadoRepository) {
        this.repository = repository;
        this.proveedorRepository = proveedorRepository;
        this.deudaProveedorRepository = deudaProveedorRepository;
        this.sueldoEmpleadoRepository = sueldoEmpleadoRepository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            log.info("[GYS-DEV] ms-finanzas ya tiene datos — se omite la carga inicial.");
            return;
        }

        LocalDate hoy = LocalDate.now();

        List<Comprobante> comprobantes = List.of(

            // Pendiente de cobro — aparato funcional en control
            Comprobante.builder()
                .nroComprobante("COMP-2025-001")
                .pedidoId(4L)
                .nroPedido("GYS-2025-0004")
                .odontologoId(4L)
                .odontologoNombre("Dra. Laura Sánchez")
                .trabajo("Aparato Funcional Bimler")
                .monto(new BigDecimal("18000.00"))
                .estadoPago(EstadoPago.PENDIENTE)
                .fechaEmision(hoy)
                .fechaVencimiento(hoy.plusDays(30))
                .build(),

            // Ya cobrado — férula entregada
            Comprobante.builder()
                .nroComprobante("COMP-2025-002")
                .pedidoId(5L)
                .nroPedido("GYS-2025-0005")
                .odontologoId(3L)
                .odontologoNombre("Dr. Martín García")
                .trabajo("Férula Miorelajante ATM")
                .monto(new BigDecimal("12000.00"))
                .estadoPago(EstadoPago.COBRADO)
                .fechaEmision(hoy.minusDays(8))
                .fechaVencimiento(hoy.minusDays(8).plusDays(30))
                .fechaCobro(hoy.minusDays(1))
                .observaciones("Pagado con transferencia.")
                .build(),

            // Vencido — corona urgente sin cobrar (para testear alertas de deuda)
            Comprobante.builder()
                .nroComprobante("COMP-2025-000")
                .pedidoId(1L)
                .nroPedido("GYS-2025-0001")
                .odontologoId(3L)
                .odontologoNombre("Dr. Martín García")
                .trabajo("Corona Metal-Cerámica")
                .monto(new BigDecimal("15000.00"))
                .estadoPago(EstadoPago.VENCIDO)
                .fechaEmision(hoy.minusDays(45))
                .fechaVencimiento(hoy.minusDays(15))
                .observaciones("Vencido — requiere gestión de cobranza.")
                .build()
        );

        repository.saveAll(comprobantes);
        log.info("[GYS-DEV] {} comprobantes de prueba cargados.", comprobantes.size());
        log.info("[GYS-DEV] Saldo pendiente Dr. García: $15.000 (vencido)");
        log.info("[GYS-DEV] Saldo pendiente Dra. Sánchez: $18.000 (vigente)");

        // ── Proveedores ────────────────────────────────────────────────
        Proveedor dentalImport = proveedorRepository.save(
            Proveedor.builder()
                .nombre("Dental Import SRL")
                .cuit("30-71234567-8")
                .build()
        );
        log.info("[GYS-DEV] Proveedor cargado: {}", dentalImport.getNombre());

        // ── Deuda proveedor ────────────────────────────────────────────
        deudaProveedorRepository.save(
            DeudaProveedor.builder()
                .proveedor(dentalImport)
                .descripcion("Cerámica Vita PM9 — Lote 2025-05")
                .monto(new BigDecimal("25000.00"))
                .fechaVencimiento(hoy.plusDays(30))
                .build()
        );
        log.info("[GYS-DEV] Deuda proveedor cargada: Cerámica Vita PM9 $25.000");

        // ── Sueldos del mes actual ──────────────────────────────────────
        int mes = hoy.getMonthValue();
        int anio = hoy.getYear();
        sueldoEmpleadoRepository.saveAll(List.of(
            SueldoEmpleado.builder()
                .empleadoId(1L)
                .empleadoNombre("Carlos López")
                .monto(new BigDecimal("180000.00"))
                .mes(mes)
                .anio(anio)
                .build(),
            SueldoEmpleado.builder()
                .empleadoId(2L)
                .empleadoNombre("Valentina Torres")
                .monto(new BigDecimal("150000.00"))
                .mes(mes)
                .anio(anio)
                .build()
        ));
        log.info("[GYS-DEV] Sueldos del mes {}/{} cargados.", mes, anio);
    }
}
