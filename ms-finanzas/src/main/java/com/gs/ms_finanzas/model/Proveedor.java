package com.gs.ms_finanzas.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Proveedor de materiales dentales del laboratorio.
 *
 * <p>Puede ser una empresa (ej: distribuidora de yesos, resinas, metales) o una persona
 * que suministra insumos específicos. Los proveedores no se eliminan físicamente;
 * se dan de baja lógicamente ({@link #activo} = false) para conservar el historial
 * de {@link DeudaProveedor}.</p>
 *
 * <p>El número de teléfono del proveedor puede coincidir con el que el bot de WhatsApp
 * usa para identificarlo como receptor en un pago triangulado.</p>
 */
@Entity
@Table(name = "proveedores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Razón social o nombre comercial del proveedor (ej: "Dentsply Argentina SA").
     */
    @Column(nullable = false, length = 200)
    private String nombre;

    /**
     * CUIT del proveedor (sin guiones, ej: "30712345671"). Puede ser nulo
     * para proveedores informales o personas físicas que aún no lo registraron.
     */
    @Column(length = 20)
    private String cuit;

    /**
     * Correo electrónico de contacto del proveedor.
     */
    @Column(length = 100)
    private String email;

    /**
     * Número de teléfono de contacto del proveedor. Puede usarse por el bot
     * para identificar pagos triangulados dirigidos a este proveedor.
     */
    @Column(length = 20)
    private String telefono;

    /**
     * Dirección postal o de depósito del proveedor. Informativa.
     */
    @Column(length = 300)
    private String direccion;

    /**
     * Indica si el proveedor está activo. Un proveedor inactivo (baja lógica)
     * no aparece en el ABM pero mantiene su historial de deudas.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    /**
     * Timestamp de alta del proveedor en el sistema. Inmutable.
     */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}
