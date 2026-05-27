package com.gys.ms_finanzas.controller;

import com.gys.ms_finanzas.dto.CobroRequest;
import com.gys.ms_finanzas.dto.RegistroCobroResponse;
import com.gys.ms_finanzas.service.ICobroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/finanzas/cobros")
@RequiredArgsConstructor
public class CobroController {

    private final ICobroService service;

    @PostMapping
    public ResponseEntity<RegistroCobroResponse> registrarCobro(@Valid @RequestBody CobroRequest request) {
        return ResponseEntity.ok(service.registrarCobro(request));
    }
}
