package com.gs.ms_catalogo.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un tipo de trabajo dental ofrecido por el laboratorio.
 * <p>
 * Cada instancia define una «plantilla» de trabajo (por ejemplo, «Corona Metal-Cerámica»,
 * «Prótesis Completa Superior», «Férula de Descarga»). Contiene:
 * <ul>
 *   <li>Datos descriptivos: nombre, descripción, categoría y foto referencial.</li>
 *   <li>Precio de referencia para cotizaciones.</li>
 *   <li>Tiempo estimado de elaboración en días.</li>
 *   <li>Receta de materiales ({@link IngredienteReceta}): lista de insumos de stock
 *       que se consumen al fabricar una unidad de este trabajo.</li>
 * </ul>
 * </p>
 * <p>
 * La baja es lógica (campo {@code activo}); los registros nunca se eliminan físicamente
 * para preservar trazabilidad histórica en órdenes y presupuestos ya emitidos.
 * </p>
 *
 * @see Categoria
 * @see IngredienteReceta
 */
@Entity
@Table(name = "tipos_trabajo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoTrabajo {

    /** Clave primaria autoincremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre comercial del trabajo dental, por ejemplo "Corona Zirconio" o
     * "Prótesis Parcial Acrílica". Debe ser único en la tabla.
     */
    @Column(nullable = false, length = 200)
    private String nombre;

    /**
     * Descripción extendida del trabajo: técnica utilizada, indicaciones clínicas,
     * materiales predominantes, etc. Opcional.
     */
    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Precio de referencia unitario en la moneda local (ARS).
     * Es orientativo; el precio final puede ajustarse por paciente o acuerdo comercial.
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal precio;

    /**
     * Categoría odontológica del trabajo.
     * Determina el grupo de visualización en la UI y puede usarse para filtrar
     * los trabajos asignables según la especialidad del odontólogo.
     *
     * @see Categoria
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    /**
     * Tiempo estimado de elaboración expresado en días hábiles de laboratorio.
     * Se utiliza para calcular la fecha de entrega prometida al odontólogo.
     */
    @Column(name = "tiempo_estimado_dias")
    private Integer tiempoEstimadoDias;

    /**
     * URL o contenido base64 de una imagen representativa del trabajo terminado.
     * Permite al odontólogo seleccionar trabajos visualmente desde la UI.
     */
    @Column(name = "foto_url", columnDefinition = "TEXT")
    private String fotoUrl;

    /**
     * Indica si el tipo de trabajo está disponible para ser asignado en nuevas órdenes.
     * Cuando se «elimina» un trabajo, este campo pasa a {@code false} (baja lógica).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    /**
     * Receta: materiales que se consumen al fabricar este tipo de trabajo.
     * Cascade ALL + orphanRemoval para que se persistan/borren junto con el TipoTrabajo.
     */
    @OneToMany(mappedBy = "tipoTrabajo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<IngredienteReceta> receta = new ArrayList<>();

    /** Timestamp de creación del registro. Se asigna automáticamente en {@link #onCreate()}. */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /** Timestamp de la última modificación. Se actualiza automáticamente en {@link #onUpdate()}. */
    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    // ── Helpers para mantener consistencia con la receta ───────────

    /**
     * Reemplaza completamente la receta de este tipo de trabajo.
     * <p>
     * Limpia la lista actual (JPA propagará los DELETE por {@code orphanRemoval = true})
     * y agrega los nuevos ingredientes asignándoles la referencia inversa correcta.
     * </p>
     *
     * @param nuevos lista de ingredientes que reemplazará la receta actual; puede ser {@code null}
     */
    public void reemplazarReceta(List<IngredienteReceta> nuevos) {
        this.receta.clear();
        if (nuevos != null) {
            for (IngredienteReceta i : nuevos) {
                i.setTipoTrabajo(this);
                this.receta.add(i);
            }
        }
    }

    /** Inicializa los timestamps de auditoría al persistir por primera vez. */
    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaModificacion = LocalDateTime.now();
    }

    /** Actualiza el timestamp de última modificación en cada UPDATE. */
    @PreUpdate
    protected void onUpdate() {
        this.fechaModificacion = LocalDateTime.now();
    }
}
