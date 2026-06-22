package com.gs.ms_finanzas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Sueldo mensual devengado por un empleado del laboratorio dental.
 *
 * <p>Representa el haber generado por empleado en un período (año/mes).
 * A diferencia de {@link ConfiguracionSueldo} (que guarda el saldo acumulado),
 * esta entidad es el registro histórico mensual, similar a un recibo de sueldo.</p>
 *
 * <p>Flujo:
 * <ol>
 *   <li>Al cerrar el mes, se genera un {@code SueldoEmpleado} con estado {@link EstadoSueldo#PENDIENTE}.</li>
 *   <li>Cuando el laboratorio efectúa el pago, se actualiza a {@link EstadoSueldo#PAGADO}.</li>
 * </ol></p>
 *
 * <p>Para la gestión avanzada con algoritmo de cascada y pagos del bot,
 * ver {@link ConfiguracionSueldo} y {@link PagoSueldo}.</p>
 */
@Entity
@Table(name = "sueldos_empleados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SueldoEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID del empleado en ms-auth.
     */
    @Column(name = "empleado_id", nullable = false)
    private Long empleadoId;

    /**
     * Nombre del empleado. Denormalizado para mostrar el historial sin dependencia de ms-auth.
     */
    @Column(name = "empleado_nombre", nullable = false, length = 150)
    private String empleadoNombre;

    /**
     * Monto bruto del sueldo mensual devengado, en pesos argentinos.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /**
     * Mes del período (1 = enero, 12 = diciembre).
     */
    @Column(nullable = false)
    private int mes;

    /**
     * Año del período (ej: 2024).
     */
    @Column(nullable = false)
    private int anio;

    /**
     * Estado del sueldo mensual.
     * <ul>
     *   <li>{@link EstadoSueldo#PENDIENTE} — devengado pero no cobrado (valor por defecto).</li>
     *   <li>{@link EstadoSueldo#PAGADO} — el laboratorio ya realizó el pago.</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private EstadoSueldo estado = EstadoSueldo.PENDIENTE;

    /**
     * Fecha en la que se efectuó el pago. Se completa al pasar a {@link EstadoSueldo#PAGADO}.
     */
    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    /**
     * Número o código del comprobante de pago (ej: número de transferencia bancaria).
     */
    @Column(name = "referencia_comprobante", length = 50)
    private String referenciaComprobante;

    /**
     * Notas adicionales sobre el pago de sueldo (ej: "Pago en dos cuotas por acuerdo").
     */
    @Column(length = 300)
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
