package com.gys.ms_finanzas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Configuración de sueldo de un integrante del laboratorio.
 *
 * Está ligada a un usuario del sistema vía {@link #empleadoId} (el id de
 * ms-auth). Guarda la config (frecuencia + monto) y el estado de cuenta del
 * empleado: cuánto se le debe (devengado) y cuánto se le pagó de más (sobrante).
 *
 * Hay una sola fila por empleado (empleadoId es único).
 */
@Entity
@Table(name = "configuracion_sueldo",
       uniqueConstraints = @UniqueConstraint(columnNames = "empleado_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionSueldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Id del usuario en ms-auth (el integrante del laboratorio). */
    @Column(name = "empleado_id", nullable = false, unique = true)
    private Long empleadoId;

    @Column(name = "empleado_nombre", nullable = false, length = 150)
    private String empleadoNombre;

    @Column(name = "rol", length = 30)
    private String rol;

    /** Teléfono — usado también por el bot para identificar quién carga/recibe. */
    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "frecuencia", nullable = false, length = 12)
    @Builder.Default
    private FrecuenciaPago frecuencia = FrecuenciaPago.MENSUAL;

    /** Monto que cobra por un ciclo completo según su frecuencia. */
    @Column(name = "monto_base", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoBase = BigDecimal.ZERO;

    /** Lo que el laboratorio le debe acumulado (positivo = a favor del empleado). */
    @Column(name = "saldo_devengado", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldoDevengado = BigDecimal.ZERO;

    /** Lo que se le pagó de más, a descontar del próximo ciclo. */
    @Column(name = "saldo_sobrante", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldoSobrante = BigDecimal.ZERO;

    @Column(name = "ultimo_pago")
    private LocalDate ultimoPago;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaModificacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }
}
