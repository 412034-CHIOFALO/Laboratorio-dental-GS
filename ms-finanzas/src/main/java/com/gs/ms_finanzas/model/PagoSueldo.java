package com.gs.ms_finanzas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Registro histórico de un pago realizado a un integrante del laboratorio.
 *
 * <p>Cada pago descuenta del saldo devengado de su {@link ConfiguracionSueldo}.
 * Pueden existir muchos pagos por empleado (pagos parciales, distintos ciclos).
 *
 * <p>El {@link #origen} distingue si lo cargó un humano ({@link OrigenPago#MANUAL})
 * o el bot de WhatsApp ({@link OrigenPago#BOT_WHATSAPP}).
 * Cuando viene del bot, se guarda quién lo cargó ({@link #cargadoPorNombre} /
 * {@link #cargadoPorTelefono}) y el link al comprobante en MinIO.</p>
 *
 * <p>El campo {@link #idOperacion} actúa como clave anti-duplicado: si el bot
 * intenta registrar el mismo comprobante dos veces, el segundo intento se rechaza
 * con estado {@link EstadoRegistroBot#DUPLICADO}.</p>
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

    /**
     * ID del empleado en ms-auth que recibió el pago.
     */
    @Column(name = "empleado_id", nullable = false)
    private Long empleadoId;

    /**
     * Nombre del empleado. Denormalizado para mostrar el historial sin dependencia de ms-auth.
     */
    @Column(name = "empleado_nombre", nullable = false, length = 150)
    private String empleadoNombre;

    /**
     * Importe efectivamente pagado, en pesos argentinos.
     * Puede ser mayor al devengado si el odontólogo pagó de más.
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /**
     * Fecha del pago según el comprobante (fecha del negocio).
     * Si no se especifica, se toma la fecha actual al persistir.
     */
    @Column(nullable = false)
    private LocalDate fecha;

    /**
     * Origen del registro de pago.
     * <ul>
     *   <li>{@link OrigenPago#MANUAL} — cargado por un usuario desde la app.</li>
     *   <li>{@link OrigenPago#BOT_WHATSAPP} — detectado automáticamente por el bot.</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private OrigenPago origen = OrigenPago.MANUAL;

    /**
     * Política aplicada cuando el pago superó el saldo devengado.
     * <ul>
     *   <li>{@link ManejoSobrante#DESCONTAR_PROXIMO} — el excedente se acumula y descuenta del próximo ciclo.</li>
     *   <li>{@link ManejoSobrante#CUBRE_LAB} — el laboratorio absorbe la diferencia.</li>
     *   <li>{@link ManejoSobrante#DEVUELVE_EMPLEADO} — el empleado debe devolver la diferencia.</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "manejo_sobrante", length = 20)
    private ManejoSobrante manejoSobrante;

    /**
     * ID / número de operación del comprobante bancario. Único cuando viene del bot;
     * sirve para no registrar dos veces el mismo comprobante (mecanismo anti-duplicado).
     */
    @Column(name = "id_operacion", length = 60)
    private String idOperacion;

    /**
     * Excedente del pago respecto al devengado (0 si no pagó de más).
     * Este valor se transfiere al {@link ConfiguracionSueldo#getSaldoSobrante()} según la política.
     */
    @Column(name = "monto_excedente", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoExcedente = BigDecimal.ZERO;

    /**
     * Nota libre del operador sobre el pago (ej: "Pago parcial por acuerdo").
     */
    @Column(length = 300)
    private String nota;

    // ── Datos del bot (solo si origen = BOT_WHATSAPP) ──

    /**
     * Nombre del integrante del laboratorio que subió el comprobante al grupo de WhatsApp.
     * Permite rastrear quién notificó el pago.
     */
    @Column(name = "cargado_por_nombre", length = 150)
    private String cargadoPorNombre;

    /**
     * Teléfono del integrante que subió el comprobante (formato con código de país).
     */
    @Column(name = "cargado_por_telefono", length = 30)
    private String cargadoPorTelefono;

    /**
     * Nombre o alias del emisor del pago según el comprobante bancario
     * (típicamente el nombre del odontólogo que pagó).
     */
    @Column(name = "emisor", length = 150)
    private String emisor;

    /**
     * URL del comprobante almacenado en MinIO. Es una ruta interna (no pre-firmada);
     * para compartirla con el frontend se genera una URL temporal mediante el servicio.
     */
    @Column(name = "comprobante_url", length = 400)
    private String comprobanteUrl;

    /**
     * Nombre del grupo de WhatsApp desde el que se envió el comprobante.
     * Útil para auditoría en laboratorios con múltiples grupos.
     */
    @Column(name = "grupo_origen", length = 100)
    private String grupoOrigen;

    /**
     * Timestamp de creación del registro. Inmutable.
     */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.fecha == null) this.fecha = LocalDate.now();
    }
}
