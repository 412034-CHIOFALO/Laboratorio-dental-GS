package com.gs.ms_stock.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa un movimiento de stock de un material del laboratorio dental G&amp;S.
 *
 * <p>Cada vez que el stock de un {@link Material} cambia (por compra, consumo en un pedido
 * o ajuste manual) se genera un registro en esta tabla, conformando el historial de
 * trazabilidad del inventario.</p>
 *
 * <p>Los tipos de movimiento posibles se definen en {@link TipoMovimiento}:</p>
 * <ul>
 *   <li>{@code ENTRADA} — Ingreso de mercadería (compra, reposición).</li>
 *   <li>{@code SALIDA} — Consumo por pedido o uso interno.</li>
 *   <li>{@code AJUSTE} — Corrección manual del stock (inventario físico).</li>
 * </ul>
 *
 * @see Material
 * @see TipoMovimiento
 */
@Entity
@Table(name = "movimientos_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoStock {

    /** Clave primaria generada por la base de datos. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Material sobre el que se aplica el movimiento.
     * Cargado de forma perezosa para evitar consultas innecesarias.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    /**
     * Tipo de movimiento: {@code ENTRADA}, {@code SALIDA} o {@code AJUSTE}.
     *
     * @see TipoMovimiento
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    /**
     * Cantidad involucrada en el movimiento, expresada en la unidad de medida del material.
     * Siempre positiva; el signo del efecto lo determina el {@link #tipo}.
     * Para {@code AJUSTE}, representa el nuevo stock absoluto, no un delta.
     */
    @Column(nullable = false)
    private Double cantidad;

    /**
     * Valor del stock del material inmediatamente después de aplicar este movimiento.
     * Permite reconstruir la curva de stock sin recalcular todos los movimientos anteriores.
     */
    @Column(name = "stock_resultante", nullable = false)
    private Double stockResultante;

    /**
     * Descripción textual del motivo del movimiento.
     * Ejemplos: "Compra proveedor Octubre 2024", "Consumo pedido #321", "Ajuste inventario mensual".
     * Máximo 255 caracteres. Opcional.
     */
    @Column(length = 255)
    private String motivo;

    /**
     * ID del pedido que originó este movimiento de tipo {@code SALIDA}.
     * Nulo si el movimiento no está asociado a ningún pedido (entradas, ajustes o
     * salidas por uso interno).
     */
    @Column(name = "pedido_id")
    private Long pedidoId;

    /**
     * Marca de tiempo exacta en que se registró el movimiento. No es actualizable.
     * Se asigna automáticamente en {@link #onCreate()}.
     */
    @Column(name = "fecha_movimiento", nullable = false, updatable = false)
    private LocalDateTime fechaMovimiento;

    /**
     * Callback JPA que inicializa la marca de tiempo al persistir el movimiento.
     */
    @PrePersist
    protected void onCreate() {
        this.fechaMovimiento = LocalDateTime.now();
    }
}
