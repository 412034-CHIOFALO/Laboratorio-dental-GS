package com.gs.ms_finanzas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Deuda del laboratorio con un proveedor de materiales.
 *
 * <p>Se crea cuando el laboratorio recibe una factura del proveedor por la compra
 * de materiales dentales. El pago puede realizarse manualmente desde la app
 * o ser detectado por el bot de WhatsApp cuando un integrante comparte un comprobante
 * de transferencia en el grupo.</p>
 *
 * <p>Estado del ciclo:
 * <ul>
 *   <li>{@link EstadoDeuda#PENDIENTE} — deuda registrada, sin cancelar.</li>
 *   <li>{@link EstadoDeuda#PAGADO} — el pago fue confirmado.</li>
 * </ul></p>
 */
@Entity
@Table(name = "deudas_proveedores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeudaProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Proveedor al que se le debe el importe. Relación Many-to-One con {@link Proveedor}.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    /**
     * Descripción del concepto de la deuda (ej: "Compra 10 kg yeso tipo IV", "Resina Filtek Z350").
     */
    @Column(nullable = false, length = 300)
    private String descripcion;

    /**
     * Importe total de la deuda en pesos argentinos.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /**
     * Estado actual de la deuda.
     * <ul>
     *   <li>{@link EstadoDeuda#PENDIENTE} — sin pagar (valor por defecto).</li>
     *   <li>{@link EstadoDeuda#PAGADO} — cancelada.</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private EstadoDeuda estado = EstadoDeuda.PENDIENTE;

    /**
     * Fecha límite de pago según la factura del proveedor. Puede ser nula.
     */
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    /**
     * Fecha en la que se efectuó el pago al proveedor. Se completa al cambiar
     * el estado a {@link EstadoDeuda#PAGADO}.
     */
    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    /**
     * Número de factura emitida por el proveedor. Permite trazabilidad contable
     * (ej: "A-0001-00012345").
     */
    @Column(name = "nro_factura_proveedor", length = 50)
    private String nroFacturaProveedor;

    /**
     * Notas adicionales sobre la deuda (ej: "Pago en dos cuotas acordado").
     */
    @Column(name = "observaciones", length = 300)
    private String observaciones;

    /**
     * Timestamp de creación del registro. Inmutable.
     */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}
