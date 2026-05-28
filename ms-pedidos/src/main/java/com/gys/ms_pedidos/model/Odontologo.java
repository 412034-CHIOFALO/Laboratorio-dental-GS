package com.gys.ms_pedidos.model;

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
    @Index(name = "idx_odontologo_nombre", columnList = "nombre")
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

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "matricula", length = 30)
    private String matricula;

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
