package com.gs.ms_pedidos.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad principal del microservicio: representa un trabajo dental encargado
 * por un odontólogo al laboratorio G&amp;S.
 *
 * <p>Un pedido nace en estado {@link EstadoPedido#RECIBIDO} y avanza por el
 * pipeline de producción hasta {@link EstadoPedido#ENTREGADO}. Puede ser
 * cancelado desde cualquier estado previo a la entrega.</p>
 *
 * <p>El número de pedido ({@link #nroPedido}) es el identificador de negocio
 * visible para el odontólogo y el personal del laboratorio. El {@link #id} es
 * solo la clave técnica de la base de datos.</p>
 *
 * <h3>Relaciones con otros microservicios</h3>
 * <ul>
 *   <li><b>ms-auth</b>: {@link #odontologoId}, {@link #tecnicoId} — IDs de usuarios.</li>
 *   <li><b>ms-catalogo</b>: {@link #catalogoTrabajoId} — tipo de trabajo.</li>
 *   <li><b>ms-stock</b>: descuento automático al pasar a {@link EstadoPedido#EN_PROCESO}.</li>
 * </ul>
 */
@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    /** Clave técnica auto-incremental. No exponer como identificador de negocio. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Número de pedido visible para el odontólogo y el laboratorio.
     * Generado automáticamente con un formato tipo "PED-2024-0001".
     * Único e inmutable una vez creado.
     */
    @Column(name = "nro_pedido", unique = true, nullable = false, length = 20)
    private String nroPedido;

    /**
     * ID del odontólogo en ms-auth (o registro local en tabla {@code odontologos}).
     * Se usa para filtrar pedidos por cliente.
     */
    @Column(name = "odontologo_id", nullable = false)
    private Long odontologoId;

    /**
     * Nombre completo del odontólogo en el momento de crear el pedido.
     * Se desnormaliza aquí para que los reportes no dependan de ms-auth.
     */
    @Column(name = "odontologo_nombre", nullable = false, length = 150)
    private String odontologoNombre;

    /** Nombre del paciente del odontólogo para quien se realiza el trabajo. */
    @Column(name = "paciente", nullable = false, length = 150)
    private String paciente;

    /**
     * ID del tipo de trabajo en ms-catalogo (ej: "Corona de zirconio", "Prótesis total").
     * Opcional si el trabajo se describe libremente en {@link #trabajo}.
     */
    @Column(name = "catalogo_trabajo_id")
    private Long catalogoTrabajoId;

    /**
     * Descripción del trabajo dental a realizar.
     * Ej: "Corona de zirconio – molar 36", "Prótesis completa superior".
     */
    @Column(name = "trabajo", nullable = false, length = 200)
    private String trabajo;

    /**
     * ID del técnico del laboratorio asignado al trabajo en ms-auth.
     * Nulo si todavía no se asignó técnico.
     */
    @Column(name = "tecnico_id")
    private Long tecnicoId;

    /**
     * Nombre del técnico asignado (desnormalizado).
     * Se sincroniza al asignar o reasignar técnico.
     */
    @Column(name = "tecnico_nombre", length = 150)
    private String tecnicoNombre;

    /**
     * Fecha comprometida de entrega al odontólogo.
     * Se calcula en días hábiles desde la fecha de creación según la
     * configuración del laboratorio ({@code pedidos.dias-habiles-entrega}).
     */
    @Column(name = "fecha_entrega", nullable = false)
    private LocalDate fechaEntrega;

    /**
     * Estado actual del pedido dentro del pipeline de producción.
     * Flujo normal: RECIBIDO → EN_PROCESO → CONTROL → LISTO → ENTREGADO.
     * Ver {@link EstadoPedido} para la descripción de cada estado.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPedido estado;

    /**
     * Prioridad del pedido: {@link Prioridad#NORMAL} (por defecto) o
     * {@link Prioridad#URGENTE} (resaltado visualmente en el tablero kanban).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", nullable = false, length = 10)
    private Prioridad prioridad;

    /**
     * Precio acordado con el odontólogo para este trabajo, en pesos argentinos.
     * Puede ser nulo si se factura a fin de mes o bajo otro convenio.
     */
    @Column(name = "precio_acordado", precision = 12, scale = 2)
    private BigDecimal precioAcordado;

    /** Observaciones de producción: indicaciones especiales, color de cerámica, etc. */
    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    // ── Datos de entrega (se completan al pasar a ENTREGADO) ──

    /**
     * Fecha real en la que se entregó el trabajo al odontólogo.
     * Puede diferir de {@link #fechaEntrega} (la estimada) si hubo demora o entrega adelantada.
     */
    @Column(name = "fecha_entrega_real")
    private LocalDate fechaEntregaReal;

    /**
     * Nombre de la persona que retiró el trabajo.
     * Puede ser el odontólogo directamente, un asistente o una empresa de cadetería.
     * Ej: "Dr. García", "Cadetería ABC".
     */
    @Column(name = "retirado_por", length = 150)
    private String retiradoPor;

    /** Notas específicas registradas en el momento de la entrega. */
    @Column(name = "observaciones_entrega", columnDefinition = "TEXT")
    private String observacionesEntrega;

    /** Timestamp de creación del registro. Inmutable (no se actualiza en ediciones). */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /** Timestamp de la última modificación del registro. Se actualiza en cada {@code @PreUpdate}. */
    @Column(name = "fecha_ultima_modificacion")
    private LocalDateTime fechaUltimaModificacion;

    // ── Consumo de stock automático ──

    /**
     * Indica si ms-stock ya descontó los materiales según la receta del catálogo.
     * Sirve como flag de idempotencia: si el pedido transita a EN_PROCESO más de
     * una vez (error de UI, reintento de red, etc.), el descuento no se repite.
     */
    @Column(name = "stock_consumido", nullable = false)
    @Builder.Default
    private boolean stockConsumido = false;

    /**
     * Timestamp en que se realizó el descuento de stock.
     * Útil para auditoría y debugging de diferencias de inventario.
     */
    @Column(name = "fecha_stock_consumido")
    private LocalDateTime fechaStockConsumido;

    /**
     * Indica si ya se generó el comprobante (cuenta por cobrar) en ms-finanzas
     * al entregar el pedido. Flag de idempotencia para no duplicar la deuda.
     */
    @Column(name = "comprobante_generado", nullable = false)
    @Builder.Default
    private boolean comprobanteGenerado = false;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaUltimaModificacion = LocalDateTime.now();
        if (this.estado == null) this.estado = EstadoPedido.RECIBIDO;
        if (this.prioridad == null) this.prioridad = Prioridad.NORMAL;
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaUltimaModificacion = LocalDateTime.now();
    }
}
