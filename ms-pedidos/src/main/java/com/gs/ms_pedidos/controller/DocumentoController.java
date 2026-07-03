package com.gs.ms_pedidos.controller;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;

import com.gs.ms_pedidos.dto.DocumentoPedidoResponse;
import com.gs.ms_pedidos.exception.BusinessException;
import com.gs.ms_pedidos.exception.ResourceNotFoundException;
import com.gs.ms_pedidos.model.DocumentoPedido;
import com.gs.ms_pedidos.repository.DocumentoPedidoRepository;
import com.gs.ms_pedidos.repository.PedidoRepository;
import com.gs.ms_pedidos.service.MinioStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestión de documentos adjuntos a pedidos.
 * <p>
 * Los documentos (presupuestos, autorizaciones, remitos, etc.) se almacenan en MinIO
 * y se referencian en la base de datos por su objectKey. Las URLs de descarga
 * son temporales (30 minutos) para mayor seguridad.
 * </p>
 */
@Tag(name = "Documentos de pedido", description = "Gestión de documentos adjuntos a un pedido (presupuestos, autorizaciones, remitos). Los archivos se almacenan en MinIO y se acceden mediante URLs prefirmadas con expiración de 30 minutos.")
@RestController
@RequestMapping("/api/pedidos/{pedidoId}/docs")
@RequiredArgsConstructor
@Slf4j
@Validated
public class DocumentoController {

    private final DocumentoPedidoRepository docRepo;
    private final PedidoRepository pedidoRepository;
    private final MinioStorageService minioStorageService;

    @Operation(
        summary = "Listar documentos de un pedido",
        description = "Devuelve todos los documentos adjuntos al pedido, ordenados del más reciente al más antiguo. Cada documento incluye una URL preformada temporal para su descarga."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de documentos obtenida correctamente"),
        @ApiResponse(responseCode = "404", description = "Pedido no encontrado")
    })
    @GetMapping
    public ResponseEntity<List<DocumentoPedidoResponse>> listar(
            @Parameter(description = "ID del pedido", example = "42")
            @PathVariable @Positive Long pedidoId) {
        List<DocumentoPedido> docs = docRepo.findByPedidoIdOrderByFechaSubidaDesc(pedidoId);
        List<DocumentoPedidoResponse> resp = docs.stream().map(d -> toResponse(d)).collect(Collectors.toList());
        return ResponseEntity.ok(resp);
    }

    @Operation(
        summary = "Subir documento a un pedido",
        description = "Sube un archivo (PDF, imagen, etc.) como documento adjunto al pedido especificado. El archivo se almacena en MinIO. Se registra el usuario que realizó la carga."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Documento subido correctamente"),
        @ApiResponse(responseCode = "400", description = "Archivo vacío o error de lectura"),
        @ApiResponse(responseCode = "404", description = "Pedido no encontrado"),
        @ApiResponse(responseCode = "503", description = "MinIO no disponible — el archivo no pudo ser guardado")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoPedidoResponse> subir(
            @Parameter(description = "ID del pedido al que se adjunta el documento", example = "42")
            @PathVariable @Positive Long pedidoId,
            @Parameter(description = "Archivo a subir (PDF, imagen, etc.)")
            @RequestParam("file") MultipartFile file,
            Authentication auth) {

        if (!pedidoRepository.existsById(pedidoId)) {
            throw new ResourceNotFoundException("Pedido no encontrado: " + pedidoId);
        }
        if (file.isEmpty()) {
            throw new BusinessException("El archivo está vacío");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BusinessException("Error al leer el archivo");
        }

        String objectKey = minioStorageService.subir(bytes, file.getContentType(),
                file.getOriginalFilename(), pedidoId);
        if (objectKey == null) {
            throw new BusinessException("No se pudo guardar el archivo (MinIO no disponible)");
        }

        String subidoPor = auth != null ? auth.getName() : "desconocido";
        DocumentoPedido doc = DocumentoPedido.builder()
                .pedidoId(pedidoId)
                .objectKey(objectKey)
                .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "archivo")
                .contentType(file.getContentType())
                .tamanioBytes(file.getSize())
                .subidoPor(subidoPor)
                .build();

        DocumentoPedido saved = docRepo.save(doc);
        log.info("[DOC] Archivo '{}' subido al pedido {}", saved.getFileName(), pedidoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @Operation(
        summary = "Obtener URL temporal de descarga",
        description = "Genera una URL preformada (presigned URL) de MinIO con 30 minutos de validez para descargar el documento indicado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "URL generada correctamente"),
        @ApiResponse(responseCode = "404", description = "Documento no encontrado o no pertenece al pedido"),
        @ApiResponse(responseCode = "503", description = "MinIO no disponible — no se pudo generar la URL")
    })
    @GetMapping("/{docId}/url")
    public ResponseEntity<Map<String, String>> getUrl(
            @Parameter(description = "ID del pedido", example = "42")
            @PathVariable @Positive Long pedidoId,
            @Parameter(description = "ID del documento", example = "7")
            @PathVariable @Positive Long docId) {
        DocumentoPedido doc = docRepo.findById(docId)
                .filter(d -> d.getPedidoId().equals(pedidoId))
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        String url = minioStorageService.urlTemporal(doc.getObjectKey(), 30);
        if (url == null) {
            return ResponseEntity.status(503).body(Map.of("error", "MinIO no disponible"));
        }
        return ResponseEntity.ok(Map.of("url", url, "fileName", doc.getFileName()));
    }

    @Operation(
        summary = "Eliminar documento de un pedido",
        description = "Elimina el documento tanto de MinIO (objeto S3) como de la base de datos. La operación es irreversible."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Documento eliminado correctamente"),
        @ApiResponse(responseCode = "404", description = "Documento no encontrado o no pertenece al pedido")
    })
    @DeleteMapping("/{docId}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del pedido", example = "42")
            @PathVariable @Positive Long pedidoId,
            @Parameter(description = "ID del documento a eliminar", example = "7")
            @PathVariable @Positive Long docId) {
        DocumentoPedido doc = docRepo.findById(docId)
                .filter(d -> d.getPedidoId().equals(pedidoId))
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
        minioStorageService.eliminar(doc.getObjectKey());
        docRepo.delete(doc);
        log.info("[DOC] Documento {} eliminado del pedido {}", docId, pedidoId);
        return ResponseEntity.noContent().build();
    }

    private DocumentoPedidoResponse toResponse(DocumentoPedido d) {
        String url = minioStorageService.urlTemporal(d.getObjectKey(), 30);
        return new DocumentoPedidoResponse(
                d.getId(), d.getPedidoId(), d.getFileName(), d.getContentType(),
                d.getTamanioBytes(), d.getSubidoPor(), d.getFechaSubida(), url);
    }
}
