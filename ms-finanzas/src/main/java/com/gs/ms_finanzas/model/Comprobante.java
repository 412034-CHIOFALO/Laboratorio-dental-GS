package com.gs.ms_finanzas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Comprobante de deuda emitido a un odontólogo por un trabajo terminado.
 *
 * <p>El ciclo de vida de un comprobante es:
 * <ol>
 *   <li>Se <b>emite</b> al entregar el trabajo ({@link EstadoPago#PENDIENTE}).</li>
 *   <li>El odontólogo <b>paga</b> y se registra el cobro → {@link EstadoPago#COBRADO}.</li>
 *   <li>Si la fecha de vencimiento pasa sin cobrar, el sistema lo marca {@link EstadoPago#VENCIDO}.</li>
 * </ol>
 * Un comprobante corresponde a exactamente un pedido de ms-pedidos.</p>
 */
@Entity
@Table(name = "comprobantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Número legible del comprobante generado por el sistema (ej: "COMP-2024-000123").
     * Único a nivel global.
     */
    @Column(name = "nro_comprobante", unique = true, nullable = false, length = 30)
    private String nroComprobante;

    /**
     * ID del pedido en ms-pedidos que origina este comprobante.
     * Permite trazar qué trabajo se está facturando.
     */
    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    /**
     * Número legible del pedido (ej: "PED-2024-000456"). Denormalizado para
     * mostrar sin necesidad de llamar a ms-pedidos.
     */
    @Column(name = "nro_pedido", nullable = false, length = 30)
    private String nroPedido;

    /**
     * ID del odontólogo en ms-auth al que se le emite el comprobante.
     */
    @Column(name = "odontologo_id", nullable = false)
    private Long odontologoId;

    /**
     * Nombre completo del odontólogo. Denormalizado para mostrar sin dependencia
     * de ms-auth al momento de consulta.
     */
    @Column(name = "odontologo_nombre", nullable = false, length = 150)
    private String odontologoNombre;

    /**
     * Descripción del trabajo realizado (ej: "Corona de porcelana - pieza 21").
     */
    @Column(name = "trabajo", nullable = false, length = 200)
    private String trabajo;

    /**
     * Monto total a cobrar por el trabajo, en pesos argentinos.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /**
     * Monto ya pagado de este comprobante (soporta pagos parciales/mensuales).
     * El saldo pendiente es {@code monto - montoPagado}.
     */
    @Column(name = "monto_pagado", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoPagado = BigDecimal.ZERO;

    /**
     * Estado actual del comprobante en el ciclo de cobro.
     * <ul>
     *   <li>{@link EstadoPago#PENDIENTE} — emitido, sin cobrar.</li>
     *   <li>{@link EstadoPago#COBRADO} — el pago fue recibido.</li>
     *   <li>{@link EstadoPago#VENCIDO} — superó la fecha de vencimiento sin cobrar.</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoPago estadoPago;

    /**
     * Fecha en que se emitió el comprobante al odontólogo.
     */
    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    /**
     * Fecha límite de pago acordada con el odontólogo. Puede ser nula si no se
     * pactó una fecha concreta.
     */
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    /**
     * Fecha en la que se registró el cobro (se completa al pasar a {@link EstadoPago#COBRADO}).
     */
    @Column(name = "fecha_cobro")
    private LocalDate fechaCobro;

    /**
     * Notas adicionales sobre el comprobante (ej: "Ajuste por descuento acordado").
     */
    @Column(length = 255)
    private String observaciones;

    /**
     * Timestamp exacto de creación del registro. Inmutable.
     */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estadoPago == null) this.estadoPago = EstadoPago.PENDIENTE;
        if (this.montoPagado == null) this.montoPagado = BigDecimal.ZERO;
    }

    /** Saldo que todavía resta cobrar de este comprobante. */
    @Transient
    public BigDecimal getSaldoPendiente() {
        BigDecimal pagado = montoPagado != null ? montoPagado : BigDecimal.ZERO;
        return monto.subtract(pagado).max(BigDecimal.ZERO);
    }
}
