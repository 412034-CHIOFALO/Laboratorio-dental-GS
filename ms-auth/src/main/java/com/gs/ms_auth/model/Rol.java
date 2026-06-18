package com.gs.ms_auth.model;

/**
 * Roles disponibles para los usuarios del Laboratorio G&amp;S.
 * <p>
 * Spring Security los expone como authorities con el prefijo {@code ROLE_}
 * (ej: {@code ROLE_ADMIN}). El rol se incluye en el claim {@code roles} del JWT
 * como cadena simple sin prefijo.
 * </p>
 * <ul>
 *   <li>{@link #ADMIN} — administrador del sistema; puede crear y aprobar usuarios, ver auditoría y acceder a H2 console.</li>
 *   <li>{@link #TECNICO} — personal técnico con acceso a funciones operativas del laboratorio.</li>
 *   <li>{@link #ADMINISTRATIVO} — personal administrativo con acceso a gestión documental y financiera.</li>
 *   <li>{@link #ODONTOLOGO} — profesional odontológico con acceso a la agenda y expedientes clínicos.</li>
 * </ul>
 */
public enum Rol {
    /** Administrador del sistema — permisos totales. */
    ADMIN,
    /** Personal técnico del laboratorio. */
    TECNICO,
    /** Personal administrativo del laboratorio. */
    ADMINISTRATIVO,
    /** Profesional odontológico. */
    ODONTOLOGO
}
