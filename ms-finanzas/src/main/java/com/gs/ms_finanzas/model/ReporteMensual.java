package com.gs.ms_finanzas.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Metadatos de un reporte financiero mensual generado y archivado por el sistema.
 *
 * <p>El PDF en sí se almacena en MinIO; aquí se guarda su ubicación ({@code objectName})
 * y los datos de generación. Hay a lo sumo un reporte por (año, mes): al regenerarlo se
 * sobrescribe la fila y el objeto en el bucket.</p>
 */
@Entity
@Table(name = "reporte_mensual", uniqueConstraints = @UniqueConstraint(columnNames = {"anio", "mes"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteMensual {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int anio;

    @Column(nullable = false)
    private int mes;

    @Column(name = "generado_en", nullable = false)
    private LocalDateTime generadoEn;

    /** Path del PDF dentro del bucket de MinIO. */
    @Column(name = "object_name", nullable = false, length = 300)
    private String objectName;

    @Column(name = "nombre_archivo", nullable = false, length = 120)
    private String nombreArchivo;

    /** {@code true} si lo generó la tarea programada mensual; {@code false} si fue manual. */
    @Column(nullable = false)
    private boolean automatico;
}
