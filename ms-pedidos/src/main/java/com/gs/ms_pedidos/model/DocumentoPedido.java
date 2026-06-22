package com.gs.ms_pedidos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Documento adjunto a un pedido (presupuesto, autorización, remito, etc.).
 *
 * <p>El contenido binario del archivo <strong>no</strong> se almacena en la base de datos:
 * solo se guarda el {@link #objectKey} que identifica el objeto en MinIO.
 * Para acceder al archivo se genera una URL preformada temporal mediante
 * {@code MinioStorageService#urlTemporal(String, int)}.</p>
 *
 * <p>La relación con {@link Pedido} es por {@link #pedidoId} (sin FK JPA)
 * para simplificar las consultas y evitar joins innecesarios.</p>
 */
@Entity
@Table(name = "documentos_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoPedido {

    /** Clave técnica auto-incremental. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID del pedido al que pertenece este documento. */
    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    /**
     * Ruta del objeto en MinIO (bucket + prefijo + nombre único).
     * Ej: {@code pedidos/42/presupuesto_2024-08-01_abc123.pdf}.
     * Se usa para generar la URL de descarga y para eliminar el objeto.
     */
    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    /** Nombre original del archivo tal como lo subió el usuario. */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /** MIME type del archivo. Ej: {@code application/pdf}, {@code image/jpeg}. */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /** Tamaño del archivo en bytes. Útil para mostrar en la UI antes de descargar. */
    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    /** Usuario (subject del JWT) que subió el documento. Para auditoría. */
    @Column(name = "subido_por", length = 150)
    private String subidoPor;

    /** Timestamp exacto en que se almacenó el documento. Inmutable. */
    @Column(name = "fecha_subida", nullable = false, updatable = false)
    private LocalDateTime fechaSubida;

    @PrePersist
    protected void onCreate() {
        this.fechaSubida = LocalDateTime.now();
    }
}
