package com.gs.ms_finanzas.service;

import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Almacenamiento de comprobantes en MinIO.
 *
 * Sube los PDF/imágenes que llegan por el bot y guarda un nombre de objeto
 * único. Para verlos desde la app, genera una URL temporal (presigned).
 *
 * Si MinIO no está disponible, las operaciones devuelven null y el pago se
 * registra igual (sin archivo) — el almacenamiento es "best effort".
 */
@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    @Value("${gs.minio.endpoint}")          private String endpoint;
    @Value("${gs.minio.public-endpoint}")   private String publicEndpoint;
    @Value("${gs.minio.access-key}")        private String accessKey;
    @Value("${gs.minio.secret-key}")        private String secretKey;
    @Value("${gs.minio.bucket}")            private String bucket;
    @Value("${gs.minio.enabled:true}")      private boolean enabled;

    private MinioClient client;

    @PostConstruct
    void init() {
        if (!enabled) {
            log.info("[MINIO] Deshabilitado por config.");
            return;
        }
        try {
            client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            boolean existe = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!existe) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("[MINIO] Bucket '{}' creado.", bucket);
            }
            log.info("[MINIO] Conectado a {} (bucket '{}').", endpoint, bucket);
        } catch (Exception e) {
            log.warn("[MINIO] No se pudo conectar ({}). Los comprobantes no se guardarán.", e.getMessage());
            client = null;
        }
    }

    /**
     * Sube un comprobante y devuelve el nombre del objeto (path en el bucket),
     * o null si MinIO no está disponible.
     */
    public String subir(byte[] datos, String mime, String nombreOriginal) {
        if (client == null || datos == null || datos.length == 0) return null;
        try {
            LocalDate hoy = LocalDate.now();
            String objectName = String.format("comprobantes/%d/%02d/%s%s",
                    hoy.getYear(), hoy.getMonthValue(), UUID.randomUUID(), extensionDe(mime, nombreOriginal));

            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(datos), datos.length, -1)
                    .contentType(mime != null && !mime.isBlank() ? mime : "application/octet-stream")
                    .build());

            log.info("[MINIO] Comprobante guardado: {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.warn("[MINIO] Error subiendo comprobante: {}", e.getMessage());
            return null;
        }
    }

    /**
     * URL temporal (presigned) para ver el comprobante desde el navegador.
     * MinIO genera la URL con el host del endpoint interno; si {@code publicEndpoint}
     * difiere (caso Docker), reemplazamos ese host por el accesible desde el browser.
     */
    public String urlTemporal(String objectName, int minutos) {
        if (client == null || objectName == null || objectName.isBlank()) return null;
        try {
            String url = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectName)
                    .expiry(minutos, TimeUnit.MINUTES)
                    .build());
            // Si el endpoint interno difiere del público, reemplazamos el host en la URL.
            if (!endpoint.equals(publicEndpoint) && url.startsWith(endpoint)) {
                url = publicEndpoint + url.substring(endpoint.length());
            }
            return url;
        } catch (Exception e) {
            log.warn("[MINIO] Error generando URL temporal: {}", e.getMessage());
            return null;
        }
    }

    private String extensionDe(String mime, String nombre) {
        if (mime != null) {
            if (mime.contains("pdf"))  return ".pdf";
            if (mime.contains("png"))  return ".png";
            if (mime.contains("jpeg") || mime.contains("jpg")) return ".jpg";
        }
        if (nombre != null && nombre.contains(".")) {
            return nombre.substring(nombre.lastIndexOf('.'));
        }
        return "";
    }
}
