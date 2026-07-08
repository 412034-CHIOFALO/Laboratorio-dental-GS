package com.gs.ms_auth.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Representa a un integrante del Laboratorio G&amp;S registrado en el sistema.
 * <p>
 * Un usuario pasa por el siguiente ciclo de vida:
 * <ol>
 *   <li>Registro por un ADMIN → {@code enabled = false}, {@code pendienteAprobacion = true}</li>
 *   <li>Aprobación por ADMIN  → {@code enabled = true},  {@code pendienteAprobacion = false}</li>
 *   <li>Baja temporal/definitiva → {@code enabled = false}, {@code pendienteAprobacion = false}</li>
 * </ol>
 * </p>
 * <p>
 * Persiste en la tabla {@code usuarios}. El campo {@code password} se almacena
 * hasheado con BCrypt; nunca se expone en ninguna respuesta.
 * </p>
 */
@Entity
@Table(name = "usuarios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    /** Identificador auto-incremental (clave primaria). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre de usuario único en el sistema. Se usa como {@code subject} del JWT emitido.
     * No puede ser nulo ni duplicado.
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * Contraseña hasheada con BCrypt (strength 10). Nunca se serializa en respuestas REST.
     */
    @Column(nullable = false)
    private String password;

    /** Nombre de pila del integrante del laboratorio. */
    private String nombre;

    /** Apellido del integrante del laboratorio. */
    private String apellido;

    /**
     * Teléfono del integrante. Usado por el bot de WhatsApp para identificar
     * quién carga/recibe comprobantes (mapeo teléfono → usuario).
     * Puede ser {@code null} si el integrante no usa el bot.
     */
    @Column(length = 30)
    private String telefono;

    /**
     * Rol del usuario dentro del sistema. Determina los permisos en todos los microservicios.
     * <ul>
     *   <li>{@link Rol#ADMIN} — acceso total, puede aprobar usuarios y ver auditoría</li>
     *   <li>{@link Rol#TECNICO} — personal técnico del laboratorio</li>
     *   <li>{@link Rol#ADMINISTRATIVO} — personal administrativo</li>
     *   <li>{@link Rol#ODONTOLOGO} — profesional odontológico</li>
     * </ul>
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    /**
     * Indica si el usuario puede autenticarse. Es {@code false} desde el registro
     * hasta que un ADMIN lo apruebe, y vuelve a {@code false} si es dado de baja.
     */
    @Builder.Default
    private boolean enabled = false;

    /**
     * Indica que la cuenta fue registrada pero aún no fue revisada por el administrador.
     * Cuando es {@code true} y {@code enabled} es {@code false}, el login devuelve 403
     * con mensaje de cuenta pendiente de aprobación.
     */
    @Builder.Default
    private boolean pendienteAprobacion = true;

    /**
     * Indica si el usuario ya aceptó los términos y condiciones del sistema.
     * Se pide una única vez, en su primer login (no lo acepta el ADMIN que lo crea).
     */
    @Builder.Default
    private boolean terminosAceptados = false;

    /** Momento en que el usuario aceptó los términos y condiciones. {@code null} si aún no los aceptó. */
    private Instant fechaAceptacionTerminos;
}
