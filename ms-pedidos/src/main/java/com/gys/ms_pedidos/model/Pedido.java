package com.gys.ms_pedidos.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nro_pedido", unique = true, nullable = false, length = 20)
    private String nroPedido;

    // Referencia al odontólogo (usuario en ms-auth)
    @Column(name = "odontologo_id", nullable = false)
    private Long odontologoId;

    @Column(name = "odontologo_nombre", nullable = false, length = 150)
    private String odontologoNombre;

    @Column(name = "paciente", nullable = false, length = 150)
    private String paciente;

    // Referencia al tipo de trabajo (ms-catalogo)
    @Column(name = "catalogo_trabajo_id")
    private Long catalogoTrabajoId;

    @Column(name = "trabajo", nullable = false, length = 200)
    private String trabajo;

    // Referencia al técnico asignado (usuario en ms-auth)
    @Column(name = "tecnico_id")
    private Long tecnicoId;

    @Column(name = "tecnico_nombre", length = 150)
    private String tecnicoNombre;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDate fechaEntrega;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPedido estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridad", nullable = false, length = 10)
    private Prioridad prioridad;

    @Column(name = "precio_acordado", precision = 12, scale = 2)
    private BigDecimal precioAcordado;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    // ── Datos de entrega (se completan al pasar a ENTREGADO) ──
    /** Fecha real de la entrega (la {@link #fechaEntrega} era la estimada). */
    @Column(name = "fecha_entrega_real")
    private LocalDate fechaEntregaReal;

    /** Persona que retiró el trabajo (nombre o "Cadetería ABC"). */
    @Column(name = "retirado_por", length = 150)
    private String retiradoPor;

    /** Notas específicas del momento de entrega. */
    @Column(name = "observaciones_entrega", columnDefinition = "TEXT")
    private String observacionesEntrega;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_ultima_modificacion")
    private LocalDateTime fechaUltimaModificacion;

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
