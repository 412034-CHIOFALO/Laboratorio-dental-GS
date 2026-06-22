package com.gs.ms_finanzas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa un movimiento de dinero en una de las tres cajas del laboratorio dental.
 *
 * <p>El laboratorio opera con tres cajas independientes:
 * <ul>
 *   <li><b>FISICA</b>: efectivo en mano.</li>
 *   <li><b>BANCARIA</b>: cuenta bancaria del laboratorio.</li>
 *   <li><b>COMPENSACION</b>: fondo interno de compensación para pagos triangulados
 *       (odontólogos que pagan a proveedores en nombre del lab).</li>
 * </ul>
 * Cada movimiento registra si es un ingreso o egreso, el concepto, el monto y la fecha.</p>
 */
@Entity
@Table(name = "caja_movimientos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CajaMovimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Dirección del flujo de dinero.
     * <ul>
     *   <li>{@link TipoMovimientoCaja#INGRESO} — entra dinero a la caja.</li>
     *   <li>{@link TipoMovimientoCaja#EGRESO} — sale dinero de la caja.</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimientoCaja tipo;

    /**
     * Caja sobre la que se aplica el movimiento.
     * Valores posibles: {@link TipoCaja#FISICA}, {@link TipoCaja#BANCARIA}, {@link TipoCaja#COMPENSACION}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_caja", nullable = false, length = 15)
    private TipoCaja tipoCaja;

    /**
     * Descripción del motivo del movimiento (ej: "Cobro comprobante #000123",
     * "Pago sueldo marzo", "Compra materiales proveedor X").
     */
    @Column(nullable = false, length = 300)
    private String concepto;

    /**
     * Importe del movimiento expresado en pesos argentinos.
     * Siempre positivo; la dirección del flujo la indica {@link #tipo}.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /**
     * Número de operación, referencia bancaria u otro identificador externo
     * que permite trazabilidad con el sistema bancario o comprobante físico.
     */
    @Column(length = 50)
    private String referencia;

    /**
     * Fecha en la que se produjo el movimiento (fecha del negocio, no la de creación del registro).
     * Si no se especifica, se toma la fecha del día actual al persistir.
     */
    @Column(name = "fecha_movimiento", nullable = false)
    private LocalDate fechaMovimiento;

    /**
     * Nombre del usuario autenticado que registró el movimiento (claim {@code sub} del JWT).
     * Permite auditar quién realizó el ajuste manual.
     */
    @Column(name = "creado_por", length = 50)
    private String creadoPor;

    /**
     * Timestamp exacto en que se persistió el registro (UTC). Inmutable.
     */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.fechaMovimiento == null) this.fechaMovimiento = LocalDate.now();
    }
}
