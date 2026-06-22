package com.gs.ms_stock.controller;

import com.gs.ms_stock.dto.ConfiguracionAlertaRequest;
import com.gs.ms_stock.model.ConfiguracionAlerta;
import com.gs.ms_stock.repository.ConfiguracionAlertaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Configuración de Alertas", description = "Número de WhatsApp del admin para alertas de stock bajo")
@RestController
@RequestMapping("/api/stock/configuracion")
@RequiredArgsConstructor
public class ConfiguracionAlertaController {

    private final ConfiguracionAlertaRepository configRepo;

    @Operation(summary = "Obtiene la configuración de alertas de stock")
    @GetMapping
    public ResponseEntity<ConfiguracionAlerta> obtener() {
        return ResponseEntity.ok(getOrCreate());
    }

    @Operation(summary = "Actualiza la configuración de alertas (ADMIN)",
               description = "Permite configurar el número de WhatsApp del administrador y activar/desactivar las alertas de stock bajo.")
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfiguracionAlerta> actualizar(@RequestBody ConfiguracionAlertaRequest req) {
        ConfiguracionAlerta config = getOrCreate();
        config.setAdminWhatsappPhone(req.getAdminWhatsappPhone());
        config.setAlertasActivas(req.isAlertasActivas());
        return ResponseEntity.ok(configRepo.save(config));
    }

    private ConfiguracionAlerta getOrCreate() {
        return configRepo.findById(1L).orElseGet(() -> configRepo.save(
                ConfiguracionAlerta.builder().id(1L).alertasActivas(false).build()));
    }
}
