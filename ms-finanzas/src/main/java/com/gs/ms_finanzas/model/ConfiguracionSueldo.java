package com.gs.ms_finanzas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Configuración de sueldo de un integrante del laboratorio dental.
 *
 * <p>Está ligada a un usuario del sistema vía {@link #empleadoId} (el id de
 * ms-auth). Guarda la configuración (frecuencia + monto) y el estado de cuenta del
 * empleado: cuánto se le debe ({@link #saldoDevengado}) y cuánto se le pagó de más
 * ({@link #saldoSobrante}).</p>
 *
 * <p>Hay una sola fila por empleado ({@link #empleadoId} es único).
 * El algoritmo de <i>cascada</i> usa esta entidad para distribuir los fondos disponibles
 * entre todos los empleados activos en cada ciclo de pago.</p>
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

    /**
     * ID del usuario en ms-auth que representa al integrante del laboratorio.
     * Clave de negocio: único por tabla.
     */
    @Column(name = "empleado_id", nullable = false, unique = true)
    private Long empleadoId;

    /**
     * Nombre completo del empleado. Denormalizado para no depender de ms-auth en
     * consultas frecuentes.
     */
    @Column(name = "empleado_nombre", nullable = false, length = 150)
    private String empleadoNombre;

    /**
     * Rol del empleado en el laboratorio (ej: "TECNICO", "ADMINISTRATIVO").
     * Informativo, no controla permisos.
     */
    @Column(name = "rol", length = 30)
    private String rol;

    /**
     * Número de teléfono (con código de país, sin espacios) usado también por el bot
     * de WhatsApp para identificar al receptor de un pago cuando llega un comprobante.
     * Ej: "+5491112345678".
     */
    @Column(name = "telefono", length = 30)
    private String telefono;

    /**
     * Indica si el empleado está activo en el laboratorio.
     * Los inactivos no participan del algoritmo de cascada.
     */
    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    /**
     * Frecuencia con la que cobra el empleado.
     * <ul>
     *   <li>{@link FrecuenciaPago#DIARIO} — cobra por día trabajado.</li>
     *   <li>{@link FrecuenciaPago#SEMANAL} — una vez por semana.</li>
     *   <li>{@link FrecuenciaPago#QUINCENAL} — cada 15 días.</li>
     *   <li>{@link FrecuenciaPago#MENSUAL} — una vez al mes (default).</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "frecuencia", nullable = false, length = 12)
    @Builder.Default
    private FrecuenciaPago frecuencia = FrecuenciaPago.MENSUAL;

    /**
     * Monto que cobra el empleado por un ciclo completo según su {@link #frecuencia}.
     * Expresado en pesos argentinos.
     */
    @Column(name = "monto_base", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoBase = BigDecimal.ZERO;

    /**
     * Saldo acumulado que el laboratorio le debe al empleado (positivo = a favor del empleado).
     * Se incrementa con cada ciclo que se devenga y se decrementa con cada pago registrado.
     */
    @Column(name = "saldo_devengado", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldoDevengado = BigDecimal.ZERO;

    /**
     * Excedente acumulado: lo que se le pagó de más al empleado, a descontar del próximo ciclo.
     * Se genera cuando el pago supera el devengado y la política es {@link ManejoSobrante#DESCONTAR_PROXIMO}.
     */
    @Column(name = "saldo_sobrante", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal saldoSobrante = BigDecimal.ZERO;

    /**
     * Fecha del último pago registrado a este empleado. Permite calcular
     * cuántos días lleva sin cobrar.
     */
    @Column(name = "ultimo_pago")
    private LocalDate ultimoPago;

    /**
     * Timestamp de creación del registro. Inmutable.
     */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Timestamp de la última modificación de la configuración.
     */
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
