package com.gs.ms_pedidos.service;

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

@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    @Value("${gs.minio.endpoint}")        private String endpoint;
    @Value("${gs.minio.public-endpoint}") private String publicEndpoint;
    @Value("${gs.minio.access-key}")      private String accessKey;
    @Value("${gs.minio.secret-key}")      private String secretKey;
    @Value("${gs.minio.bucket}")          private String bucket;
    @Value("${gs.minio.enabled:true}")    private boolean enabled;

    private MinioClient client;

    @PostConstruct
    void init() {
        if (!enabled) {
            log.info("[MINIO-DOCS] Deshabilitado por config.");
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
                log.info("[MINIO-DOCS] Bucket '{}' creado.", bucket);
            }
            log.info("[MINIO-DOCS] Conectado a {} (bucket '{}').", endpoint, bucket);
        } catch (Exception e) {
            log.warn("[MINIO-DOCS] No se pudo conectar ({}). Los documentos no se guardarán.", e.getMessage());
            client = null;
        }
    }

    public String subir(byte[] datos, String mime, String nombreOriginal, Long pedidoId) {
        return subir(datos, mime, nombreOriginal, pedidoId, "pedidos");
    }

    public String subir(byte[] datos, String mime, String nombreOriginal, Long pedidoId, String prefijo) {
        if (client == null || datos == null || datos.length == 0) return null;
        try {
            LocalDate hoy = LocalDate.now();
            String objectName = String.format("%s/%d/%d/%02d/%s%s",
                    prefijo, pedidoId, hoy.getYear(), hoy.getMonthValue(),
                    UUID.randomUUID(), extensionDe(mime, nombreOriginal));

            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(new ByteArrayInputStream(datos), datos.length, -1)
                    .contentType(mime != null && !mime.isBlank() ? mime : "application/octet-stream")
                    .build());

            log.info("[MINIO-DOCS] Archivo guardado: {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.warn("[MINIO-DOCS] Error subiendo archivo: {}", e.getMessage());
            return null;
        }
    }

    public String urlTemporal(String objectName, int minutos) {
        if (client == null || objectName == null || objectName.isBlank()) return null;
        try {
            String url = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectName)
                    .expiry(minutos, TimeUnit.MINUTES)
                    .build());
            if (!endpoint.equals(publicEndpoint) && url.startsWith(endpoint)) {
                url = publicEndpoint + url.substring(endpoint.length());
            }
            return url;
        } catch (Exception e) {
            log.warn("[MINIO-DOCS] Error generando URL temporal: {}", e.getMessage());
            return null;
        }
    }

    public void eliminar(String objectName) {
        if (client == null || objectName == null || objectName.isBlank()) return;
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectName).build());
            log.info("[MINIO-DOCS] Documento eliminado: {}", objectName);
        } catch (Exception e) {
            log.warn("[MINIO-DOCS] Error eliminando documento: {}", e.getMessage());
        }
    }

    private String extensionDe(String mime, String nombre) {
        if (mime != null) {
            if (mime.contains("pdf"))                         return ".pdf";
            if (mime.contains("png"))                         return ".png";
            if (mime.contains("jpeg") || mime.contains("jpg")) return ".jpg";
            if (mime.contains("gif"))                         return ".gif";
            if (mime.contains("webp"))                        return ".webp";
        }
        if (nombre != null && nombre.contains(".")) {
            return nombre.substring(nombre.lastIndexOf('.'));
        }
        return "";
    }
}
