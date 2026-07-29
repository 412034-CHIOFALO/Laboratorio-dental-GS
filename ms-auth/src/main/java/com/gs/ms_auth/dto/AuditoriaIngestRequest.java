package com.gs.ms_auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Evento de auditoría que un microservicio le envía a ms-auth para centralizar.
 *
 * <p>Cada MS arma este payload al ocurrir una operación crítica (pago, entrega,
 * movimiento de caja, cambio de stock, etc.) y lo manda a
 * {@code POST /api/auth/auditoria/ingest}. El {@code usuario} lo resuelve el MS
 * origen desde el JWT del request (o "sistema"/"bot" para acciones automáticas).</p>
 */
public record AuditoriaIngestRequest(
        @NotBlank @Size(max = 100) String usuario,
        @NotBlank @Size(max = 50)  String tipo,
        @NotBlank @Size(max = 200) String accion,
        @NotBlank @Size(max = 200) String entidad,
        @Size(max = 500)           String detalle
) {}
