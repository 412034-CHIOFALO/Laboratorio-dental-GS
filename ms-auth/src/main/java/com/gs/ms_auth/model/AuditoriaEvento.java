package com.gs.ms_auth.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Registro inmutable de un evento ocurrido en el sistema del Laboratorio G&amp;S.
 * <p>
 * Los eventos se insertan desde cualquier microservicio vía {@link com.gs.ms_auth.service.AuditoriaService}
 * y nunca se modifican ni eliminan, garantizando una bitácora de auditoría confiable.
 * Persiste en la tabla {@code auditoria_eventos}.
 * </p>
 */
@Entity
@Table(name = "auditoria_eventos")
@Data
@NoArgsConstructor
public class AuditoriaEvento {

    /** Identificador auto-incremental (clave primaria). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Momento UTC exacto en que ocurrió el evento.
     * Se establece automáticamente en el constructor a {@code Instant.now()}.
     */
    @Column(nullable = false)
    private Instant timestamp;

    /**
     * Username del usuario que realizó la acción.
     * Corresponde al campo {@code sub} del JWT o al username que intentó el login.
     */
    @Column(nullable = false, length = 100)
    private String usuario;

    /**
     * Categoría del evento. Valores posibles del dominio:
     * {@code LOGIN}, {@code CREAR}, {@code EDITAR}, {@code ELIMINAR}, {@code PAGO}, {@code ESTADO}.
     */
    @Column(nullable = false, length = 50)
    private String tipo;

    /**
     * Descripción breve de la operación realizada (ej: "Inicio de sesión", "Activación de usuario").
     */
    @Column(nullable = false, length = 200)
    private String accion;

    /**
     * Nombre de la entidad afectada por el evento (ej: "Usuario jperez", "Sesión", "Pago #123").
     */
    @Column(nullable = false, length = 200)
    private String entidad;

    /**
     * Información adicional de contexto. Puede contener roles, valores antes/después, etc.
     * Nunca es {@code null} en base de datos — se guarda como cadena vacía si no hay detalle.
     */
    @Column(length = 500)
    private String detalle;

    public AuditoriaEvento(String usuario, String tipo, String accion, String entidad, String detalle) {
        this.timestamp = Instant.now();
        this.usuario   = usuario;
        this.tipo      = tipo;
        this.accion    = accion;
        this.entidad   = entidad;
        this.detalle   = detalle != null ? detalle : "";
    }
}
