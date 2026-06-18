package com.gs.ms_finanzas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bitácora de todo lo que procesa el bot de WhatsApp a partir de un comprobante de pago.
 *
 * <p>Registra los tres posibles resultados:
 * <ul>
 *   <li><b>REGISTRADO</b> — el pago fue aplicado (a un empleado o proveedor).</li>
 *   <li><b>RECHAZADO</b> — no se pudo resolver al receptor (nombre desconocido).</li>
 *   <li><b>DUPLICADO</b> — el nro de operación ya había sido registrado previamente.</li>
 * </ul>
 *
 * <p>Es la fuente del "historial del bot" que se visualiza en el frontend,
 * dando visibilidad de cómo el bot interpreta cada comprobante compartido en el grupo
 * de WhatsApp. También incluye pagos a proveedores (no solo sueldos).</p>
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

    /**
     * Timestamp exacto en que el bot procesó el comprobante. Inmutable.
     */
    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    /**
     * Monto extraído del comprobante bancario, en pesos argentinos.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal monto;

    /**
     * Número de operación del comprobante bancario (ej: CVU/CBU de la transferencia).
     * Clave de anti-duplicado: si el bot recibe el mismo comprobante dos veces,
     * el segundo intento resulta en {@link EstadoRegistroBot#DUPLICADO}.
     */
    @Column(name = "id_operacion", length = 60)
    private String idOperacion;

    /**
     * Nombre del emisor del pago ("De:") tal como figura en el comprobante o
     * en el pie del mensaje del grupo de WhatsApp.
     */
    @Column(length = 200)
    private String emisor;

    /**
     * Nombre del receptor tal como vino en el comprobante ("Para:"),
     * antes de que el bot intente resolverlo contra empleados o proveedores.
     */
    @Column(name = "receptor_nombre", length = 200)
    private String receptorNombre;

    /**
     * Tipo de receptor que el bot identificó.
     * <ul>
     *   <li>{@link TipoReceptorBot#EMPLEADO} — pago de sueldo a un integrante del lab.</li>
     *   <li>{@link TipoReceptorBot#PROVEEDOR} — pago a un proveedor (insumos / triangulado).</li>
     *   <li>{@link TipoReceptorBot#DESCONOCIDO} — no se encontró match.</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_receptor", length = 15)
    private TipoReceptorBot tipoReceptor;

    /**
     * ID del empleado o proveedor con el que se hizo match (si lo hubo).
     * Nulo si {@link #tipoReceptor} es {@link TipoReceptorBot#DESCONOCIDO}.
     */
    @Column(name = "receptor_id")
    private Long receptorId;

    /**
     * Nombre del empleado o proveedor resuelto por el bot (si hubo match).
     * Permite mostrar "Pago a: <nombre resuelto>" en el historial del front.
     */
    @Column(name = "receptor_resuelto", length = 200)
    private String receptorResuelto;

    /**
     * Resultado del procesamiento del comprobante.
     * <ul>
     *   <li>{@link EstadoRegistroBot#REGISTRADO} — pago aplicado exitosamente.</li>
     *   <li>{@link EstadoRegistroBot#RECHAZADO} — receptor no identificado.</li>
     *   <li>{@link EstadoRegistroBot#DUPLICADO} — número de operación ya procesado.</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoRegistroBot estado;

    /**
     * Mensaje descriptivo del resultado (ej: "Sueldo registrado para Juan Perez — $45.000"
     * o "Receptor desconocido: 'Lab Dental XYZ' no matchea con ningún empleado ni proveedor").
     */
    @Column(length = 300)
    private String mensaje;

    // ── Trazabilidad ──

    /**
     * Nombre del integrante del laboratorio que subió el comprobante al grupo.
     */
    @Column(name = "cargado_por_nombre", length = 150)
    private String cargadoPorNombre;

    /**
     * Teléfono del integrante que subió el comprobante (formato con código de país).
     */
    @Column(name = "cargado_por_telefono", length = 30)
    private String cargadoPorTelefono;

    /**
     * Nombre del grupo de WhatsApp desde el que llegó el comprobante.
     */
    @Column(name = "grupo_origen", length = 150)
    private String grupoOrigen;

    /**
     * Ruta interna del comprobante guardado en MinIO. Para obtener una URL
     * temporal (pre-firmada) se debe llamar al endpoint correspondiente.
     */
    @Column(name = "comprobante_url", length = 300)
    private String comprobanteUrl;

    @PrePersist
    protected void onCreate() {
        if (this.fechaHora == null) this.fechaHora = LocalDateTime.now();
    }
}
