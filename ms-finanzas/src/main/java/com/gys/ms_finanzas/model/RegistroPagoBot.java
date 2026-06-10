package com.gys.ms_finanzas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bitácora de TODO lo que procesa el bot de WhatsApp a partir de un comprobante:
 * pagos de sueldo, pagos a proveedor y también los rechazos/duplicados.
 *
 * Es la fuente del "historial del bot" que se ve en el front, para tener
 * visibilidad de cómo el bot interpreta cada comprobante.
 */
@Entity
@Table(name = "registros_pago_bot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroPagoBot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(precision = 12, scale = 2)
    private BigDecimal monto;

    /** Nro de operación del comprobante (para anti-duplicado y trazabilidad). */
    @Column(name = "id_operacion", length = 60)
    private String idOperacion;

    /** Emisor del pago (De), del comprobante o del pie del mensaje. */
    @Column(length = 200)
    private String emisor;

    /** Receptor tal como vino (Para), antes de resolverlo. */
    @Column(name = "receptor_nombre", length = 200)
    private String receptorNombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_receptor", length = 15)
    private TipoReceptorBot tipoReceptor;

    /** Id del empleado o proveedor con el que se matcheó (si hubo match). */
    @Column(name = "receptor_id")
    private Long receptorId;

    /** Nombre del empleado/proveedor resuelto (si hubo match). */
    @Column(name = "receptor_resuelto", length = 200)
    private String receptorResuelto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoRegistroBot estado;

    /** Mensaje del resultado (ej: "Sueldo registrado..." o el motivo del rechazo). */
    @Column(length = 300)
    private String mensaje;

    // ── Trazabilidad ──
    @Column(name = "cargado_por_nombre", length = 150)
    private String cargadoPorNombre;

    @Column(name = "cargado_por_telefono", length = 30)
    private String cargadoPorTelefono;

    @Column(name = "grupo_origen", length = 150)
    private String grupoOrigen;

    /** Referencia al archivo del comprobante guardado en MinIO. */
    @Column(name = "comprobante_url", length = 300)
    private String comprobanteUrl;

    @PrePersist
    protected void onCreate() {
        if (this.fechaHora == null) this.fechaHora = LocalDateTime.now();
    }
}
