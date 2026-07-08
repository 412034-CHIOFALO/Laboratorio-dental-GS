package com.gs.ms_finanzas.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pago que un odontólogo realiza a su cuenta corriente. Es independiente de un
 * comprobante puntual: el monto se imputa a las deudas pendientes (de la más
 * vieja a la más nueva), pudiendo cubrir total o parcialmente varias.
 *
 * <p>Sirve de histórico de cobranzas y de respaldo del ingreso de caja.</p>
 */
@Entity
@Table(name = "pagos_cuenta_corriente")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagoCuentaCorriente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "odontologo_id", nullable = false)
    private Long odontologoId;

    @Column(name = "odontologo_nombre", nullable = false, length = 150)
    private String odontologoNombre;

    /** Monto total entregado por el odontólogo en este pago. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /** Parte del monto que efectivamente se imputó a deudas (≤ monto). */
    @Column(name = "monto_imputado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoImputado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MedioPago medio;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 255)
    private String nota;

    /** Quién registró el pago (usuario del panel). */
    @Column(name = "registrado_por", length = 100)
    private String registradoPor;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.fecha == null) this.fecha = LocalDate.now();
    }
}
