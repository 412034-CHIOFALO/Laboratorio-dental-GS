package com.gys.ms_finanzas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "comprobantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nro_comprobante", unique = true, nullable = false, length = 30)
    private String nroComprobante;

    // Referencia al pedido facturado (ms-pedidos)
    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "nro_pedido", nullable = false, length = 30)
    private String nroPedido;

    // Odontólogo al que se le factura
    @Column(name = "odontologo_id", nullable = false)
    private Long odontologoId;

    @Column(name = "odontologo_nombre", nullable = false, length = 150)
    private String odontologoNombre;

    @Column(name = "trabajo", nullable = false, length = 200)
    private String trabajo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pago", nullable = false, length = 20)
    private EstadoPago estadoPago;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "fecha_cobro")
    private LocalDate fechaCobro;

    @Column(length = 255)
    private String observaciones;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estadoPago == null) this.estadoPago = EstadoPago.PENDIENTE;
    }
}
