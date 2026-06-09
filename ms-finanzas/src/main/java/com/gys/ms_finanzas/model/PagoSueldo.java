package com.gys.ms_finanzas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro histórico de un pago realizado a un integrante del laboratorio.
 *
 * Cada pago descuenta del saldo devengado de su {@link ConfiguracionSueldo}.
 * Pueden existir muchos pagos por empleado (pagos parciales, distintos ciclos).
 *
 * El {@link #origen} distingue si lo cargó un humano o el bot de WhatsApp.
 * Cuando viene del bot, se guarda quién lo cargó ({@link #cargadoPorNombre} /
 * {@link #cargadoPorTelefono}) y el link al comprobante.
 */
@Entity
@Table(name = "pagos_sueldo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoSueldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empleado_id", nullable = false)
    private Long empleadoId;

    @Column(name = "empleado_nombre", nullable = false, length = 150)
    private String empleadoNombre;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private OrigenPago origen = OrigenPago.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "manejo_sobrante", length = 20)
    private ManejoSobrante manejoSobrante;

    /**
     * ID / número de operación del comprobante. Único cuando viene del bot,
     * sirve para no registrar dos veces el mismo comprobante (anti-duplicado).
     */
    @Column(name = "id_operacion", length = 60)
    private String idOperacion;

    /** Excedente del pago respecto a lo devengado (0 si no pagó de más). */
    @Column(name = "monto_excedente", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoExcedente = BigDecimal.ZERO;

    @Column(length = 300)
    private String nota;

    // ── Datos del bot (solo si origen = BOT_WHATSAPP) ──
    /** Quién mandó el comprobante al grupo (integrante del lab). */
    @Column(name = "cargado_por_nombre", length = 150)
    private String cargadoPorNombre;

    @Column(name = "cargado_por_telefono", length = 30)
    private String cargadoPorTelefono;

    /** Quién emitió el pago (típicamente un odontólogo). */
    @Column(name = "emisor", length = 150)
    private String emisor;

    @Column(name = "comprobante_url", length = 400)
    private String comprobanteUrl;

    @Column(name = "grupo_origen", length = 100)
    private String grupoOrigen;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.fecha == null) this.fecha = LocalDate.now();
    }
}
