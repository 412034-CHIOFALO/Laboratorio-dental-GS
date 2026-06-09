package com.gys.ms_stock.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "materiales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaMaterial categoria;

    // Cantidad actual en stock
    @Column(name = "stock_actual", nullable = false)
    private Double stockActual;

    // Cantidad mínima antes de alertar
    @Column(name = "stock_minimo", nullable = false)
    private Double stockMinimo;

    @Column(name = "unidad_medida", nullable = false, length = 20)
    private String unidadMedida; // g, ml, unidad, cm, etc.

    @Column(name = "precio_unitario", precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(length = 100)
    private String proveedor;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    /**
     * Si true → al usarlo en una receta se descuenta {@code cantidad} del stock.
     * Si false → solo se verifica que {@code stockActual > 0}; el descuento físico
     * lo lleva el operador a mano (caso de esmaltes, pinceles, materiales que se
     * usan "por pinceladas" y no tiene sentido decrementar numéricamente).
     */
    @Column(name = "descuenta_stock", nullable = false)
    @Builder.Default
    private boolean descuentaStock = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaModificacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }
}
