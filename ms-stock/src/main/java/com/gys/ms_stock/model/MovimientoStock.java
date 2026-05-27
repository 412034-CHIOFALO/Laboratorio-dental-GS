package com.gys.ms_stock.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_stock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo; // ENTRADA | SALIDA | AJUSTE

    @Column(nullable = false)
    private Double cantidad;

    @Column(name = "stock_resultante", nullable = false)
    private Double stockResultante;

    @Column(length = 255)
    private String motivo;

    // Si la salida es por un pedido, se guarda el ID
    @Column(name = "pedido_id")
    private Long pedidoId;

    @Column(name = "fecha_movimiento", nullable = false, updatable = false)
    private LocalDateTime fechaMovimiento;

    @PrePersist
    protected void onCreate() {
        this.fechaMovimiento = LocalDateTime.now();
    }
}
