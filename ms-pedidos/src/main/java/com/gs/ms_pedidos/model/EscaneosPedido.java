package com.gs.ms_pedidos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Escaneo 3D adjunto a un pedido (archivo STL, OBJ u otro formato digital dental).
 *
 * <p>El laboratorio recibe del odontólogo (o genera internamente) archivos digitales
 * de los modelos dentales: arcadas, antagonistas, implantes, encerados, etc.
 * Estos archivos se almacenan en MinIO bajo el prefijo {@code escaneos/}.</p>
 *
 * <p>Al igual que {@link DocumentoPedido}, el contenido binario no se persiste en la
 * base de datos: se guarda únicamente el {@link #objectKey} para referenciar el
 * objeto en MinIO.</p>
 */
@Entity
@Table(name = "escaneos_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EscaneosPedido {

    /** Clave técnica auto-incremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID del pedido al que pertenece este escaneo. */
    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    /**
     * Ruta del objeto en MinIO (bucket + prefijo {@code escaneos/} + nombre único).
     * Ej: {@code pedidos/42/escaneos/arcada_superior_abc123.stl}.
     */
    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    /** Nombre original del archivo de escaneo tal como lo subió el usuario. */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** MIME type del archivo. Ej: {@code model/stl}, {@code application/octet-stream}. */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /** Tamaño del archivo en bytes. Los archivos STL suelen ser varios MB. */
    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    /**
     * Descripción libre del escaneo para identificarlo rápidamente en la lista.
     * Ejemplos: "Arcada superior", "Modelo antagonista", "Cera de diagnóstico".
     */
    @Column(name = "descripcion", length = 255)
    private String descripcion;

    /** Usuario (subject del JWT) que subió el escaneo. Para auditoría. */
    @Column(name = "subido_por", length = 150)
    private String subidoPor;

    /** Timestamp exacto en que se almacenó el escaneo. Inmutable. */
    @Column(name = "fecha_subida", nullable = false, updatable = false)
    private LocalDateTime fechaSubida;

    @PrePersist
    protected void onCreate() {
        this.fechaSubida = LocalDateTime.now();
    }
}
