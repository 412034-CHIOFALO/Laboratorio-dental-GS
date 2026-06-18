package com.gs.ms_stock.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa un material o insumo del inventario del laboratorio dental G&amp;S.
 *
 * <p>Ejemplos de materiales: acrílico, dientes de resina, alambre ortodóntico, yeso tipo IV,
 * cera de modelado, zirconia, etc.</p>
 *
 * <p>El stock se descuenta automáticamente cuando se registra un pedido que consume este
 * material según la receta definida en el catálogo. Si {@link #descuentaStock} es {@code false},
 * el descuento numérico no se aplica y solo se verifica que haya existencia.</p>
 *
 * @see MovimientoStock
 * @see CategoriaMaterial
 */
@Entity
@Table(name = "materiales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    /** Clave primaria generada por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre descriptivo del material (p. ej. "Acrílico rosa termoformable").
     * Máximo 200 caracteres. Obligatorio.
     */
    @Column(nullable = false, length = 200)
    private String nombre;

    /**
     * Descripción extendida opcional: marca, especificaciones técnicas, observaciones de uso, etc.
     */
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Categoría funcional del material dentro del laboratorio.
     *
     * @see CategoriaMaterial
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaMaterial categoria;

    /**
     * Cantidad actual disponible en stock, expresada en la unidad de medida definida
     * en {@link #unidadMedida}. Puede llegar a valores negativos si el sistema
     * permite registrar consumos sin stock suficiente.
     */
    @Column(name = "stock_actual", nullable = false)
    private Double stockActual;

    /**
     * Umbral mínimo de stock. Cuando {@code stockActual <= stockMinimo} el material
     * aparece en el listado de alertas ({@code GET /api/stock/alertas}) y el campo
     * {@code bajoStock} del DTO de respuesta devuelve {@code true}.
     */
    @Column(name = "stock_minimo", nullable = false)
    private Double stockMinimo;

    /**
     * Unidad de medida del stock: {@code "g"} (gramos), {@code "ml"} (mililitros),
     * {@code "unidad"}, {@code "cm"}, etc. Máximo 20 caracteres.
     */
    @Column(name = "unidad_medida", nullable = false, length = 20)
    private String unidadMedida;

    /**
     * Precio de costo unitario del material en moneda local (ARS).
     * Opcional; se usa para estimación de costos de producción. Precisión: 12 enteros, 2 decimales.
     */
    @Column(name = "precio_unitario", precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    /**
     * Nombre o razón social del proveedor habitual de este material. Opcional. Máximo 100 caracteres.
     */
    @Column(length = 100)
    private String proveedor;

    /**
     * Indica si el material está disponible para ser consultado y utilizado.
     * La baja del material es lógica: se pone en {@code false} en lugar de borrar el registro,
     * para preservar el historial de movimientos.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    /**
     * Controla si el stock se decrementa numéricamente al usar este material en una receta.
     *
     * <ul>
     *   <li>{@code true} — descuento normal: {@code stockActual -= cantidadUsada} (caso habitual).</li>
     *   <li>{@code false} — solo verificación de existencia; el descuento físico lo realiza
     *       el operador de forma manual. Corresponde a materiales de uso por "pinceladas"
     *       (esmaltes, pinceles, lubricantes) donde no tiene sentido decrementar numéricamente.</li>
     * </ul>
     */
    @Column(name = "descuenta_stock", nullable = false)
    @Builder.Default
    private boolean descuentaStock = true;

    /**
     * Marca de tiempo de la primera persistencia del registro. No es actualizable.
     * Se asigna automáticamente en {@link #onCreate()}.
     */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /**
     * Marca de tiempo de la última modificación del registro.
     * Se actualiza automáticamente en {@link #onUpdate()}.
     */
    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    /**
     * Callback JPA que inicializa las marcas de tiempo al persistir por primera vez.
     */
    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaModificacion = LocalDateTime.now();
    }

    /**
     * Callback JPA que actualiza la marca de tiempo de modificación en cada {@code UPDATE}.
     */
    @PreUpdate
    protected void onUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }
}
