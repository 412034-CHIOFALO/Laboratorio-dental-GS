package com.gs.ms_catalogo.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Línea de receta de un {@link TipoTrabajo}: un material del stock que se
 * consume para fabricar ese tipo de trabajo, con su cantidad y unidad.
 *
 * Los materiales viven en ms-stock — acá solo guardamos el id como referencia
 * y un snapshot del nombre para mostrar sin necesidad de Feign en cada listado.
 * Las cantidades son referenciales (para trazabilidad), no bloquean producción.
 */
@Entity
@Table(name = "ingredientes_receta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngredienteReceta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK al tipo de trabajo dueño de la receta. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_trabajo_id", nullable = false)
    private TipoTrabajo tipoTrabajo;

    /** Referencia al Material que vive en ms-stock (cross-ms by id). */
    @Column(name = "material_id", nullable = false)
    private Long materialId;

    /** Snapshot del nombre para mostrar sin llamar a ms-stock. */
    @Column(name = "material_nombre", nullable = false, length = 200)
    private String materialNombre;

    /** Unidad de medida del material (snapshot — "gr", "ml", "unidad"). */
    @Column(length = 30)
    private String unidad;

    /**
     * Cantidad de material que se consume por unidad fabricada de este tipo de trabajo.
     * La unidad se especifica en {@link #unidad}. Valor siempre positivo.
     * Ejemplo: 15.000 (gramos de zirconio para una corona).
     */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    /** Notas opcionales: "color A2", "vita 3D", etc. */
    @Column(columnDefinition = "TEXT")
    private String notas;
}
