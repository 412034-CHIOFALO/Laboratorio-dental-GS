package com.gs.ms_pedidos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Odontólogo cliente del laboratorio.
 *
 * Se buscan/crean por nombre desde el flujo "Nuevo pedido":
 *   - Si el nombre tipeado coincide con uno existente → se reutiliza.
 *   - Si no coincide → se crea uno nuevo automáticamente.
 *
 * Los datos de contacto (teléfono, email) son opcionales — se completan
 * después desde la pantalla de fichas si hace falta.
 */
@Entity
@Table(name = "odontologos", indexes = {
    @Index(name = "idx_odontologo_nombre",    columnList = "nombre"),
    @Index(name = "idx_odontologo_dni",       columnList = "dni"),
    @Index(name = "idx_odontologo_cuit",      columnList = "cuit"),
    @Index(name = "idx_odontologo_matricula", columnList = "matricula")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Odontologo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    /** Documento de identidad (solo dígitos, 7-8 caracteres). Único cuando está seteado. */
    @Column(name = "dni", length = 10, unique = true)
    private String dni;

    /** CUIT formato XX-XXXXXXXX-X. Único cuando está seteado. */
    @Column(name = "cuit", length = 13, unique = true)
    private String cuit;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "email", length = 100)
    private String email;

    /** Matrícula profesional (ej: "MN 12345", "MP 8901"). */
    @Column(name = "matricula", length = 30)
    private String matricula;

    /** Clínica u hospital donde atiende. */
    @Column(name = "clinica", length = 200)
    private String clinica;

    /** Dirección del consultorio (calle, número, ciudad). */
    @Column(name = "direccion", length = 250)
    private String direccion;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaModificacion = LocalDateTime.now();
        if (this.activo == null) this.activo = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }
}
