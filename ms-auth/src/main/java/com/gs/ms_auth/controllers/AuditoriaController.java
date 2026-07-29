package com.gs.ms_auth.controllers;

import com.gs.ms_auth.dto.AuditoriaIngestRequest;
import com.gs.ms_auth.service.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para la bitácora de auditoría del sistema.
 * <p>
 * Todos los endpoints de este controlador requieren rol ADMIN. Los eventos
 * son inmutables: solo se insertan, nunca se modifican ni eliminan.
 * </p>
 */
@Tag(name = "Auditoría", description = "Bitácora inmutable de eventos del sistema (solo administradores)")
@RestController
@RequestMapping("/api/auth")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Operation(
        summary = "Listar todos los eventos de auditoría",
        description = "Devuelve la bitácora completa de eventos del sistema en orden cronológico descendente. " +
                      "Incluye logins, creaciones, ediciones de usuarios y cambios de estado. " +
                      "Requiere rol ADMIN (Bearer JWT con claim roles=ROLE_ADMIN)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de eventos devuelta correctamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "[{\"id\":1,\"timestamp\":\"2025-06-17T10:00:00Z\"," +
                        "\"usuario\":\"admin\",\"tipo\":\"LOGIN\",\"accion\":\"Inicio de sesión\"," +
                        "\"entidad\":\"Sesión\",\"detalle\":\"Login exitoso · roles: ROLE_ADMIN\"}]"))),
        @ApiResponse(responseCode = "403", description = "Sin permisos de administrador", content = @Content)
    })
    @GetMapping("/auditoria")
    public ResponseEntity<List<Map<String, Object>>> listar() {
        List<Map<String, Object>> eventos = auditoriaService.listarTodos().stream()
            .map(e -> {
                Map<String, Object> m = new HashMap<>();
                m.put("id",        e.getId());
                m.put("timestamp", e.getTimestamp().toString());
                m.put("usuario",   e.getUsuario());
                m.put("tipo",      e.getTipo());
                m.put("accion",    e.getAccion());
                m.put("entidad",   e.getEntidad());
                m.put("detalle",   e.getDetalle() != null ? e.getDetalle() : "");
                return m;
            })
            .toList();
        return ResponseEntity.ok(eventos);
    }

    @Operation(
        summary = "Ingesta de un evento de auditoría desde otro microservicio",
        description = "Centraliza en ms-auth los eventos de auditoría del resto del sistema (pagos, entregas, " +
                      "movimientos de caja, cambios de stock, etc.). Se autentica por key interna " +
                      "(header X-Internal-Key), no por JWT de usuario — lo llaman los MS server-to-server. " +
                      "El campo 'usuario' lo resuelve el MS origen desde el JWT del request."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Evento registrado"),
        @ApiResponse(responseCode = "403", description = "Key interna inválida o ausente", content = @Content)
    })
    @PostMapping("/auditoria/ingest")
    public ResponseEntity<Void> ingerir(@Valid @RequestBody AuditoriaIngestRequest req) {
        auditoriaService.registrar(req.usuario(), req.tipo(), req.accion(), req.entidad(), req.detalle());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
