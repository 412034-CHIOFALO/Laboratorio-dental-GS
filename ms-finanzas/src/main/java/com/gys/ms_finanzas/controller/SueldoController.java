package com.gys.ms_finanzas.controller;

import com.gys.ms_finanzas.dto.SueldoRequest;
import com.gys.ms_finanzas.dto.SueldoResponse;
import com.gys.ms_finanzas.service.ISueldoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finanzas/sueldos")
@RequiredArgsConstructor
public class SueldoController {

    private final ISueldoService service;

    @GetMapping
    public ResponseEntity<List<SueldoResponse>> listar(@RequestParam int anio, @RequestParam int mes) {
        return ResponseEntity.ok(service.listarPorMes(anio, mes));
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<SueldoResponse>> pendientes(@RequestParam int anio, @RequestParam int mes) {
        return ResponseEntity.ok(service.listarPendientesMes(anio, mes));
    }

    @PostMapping
    public ResponseEntity<SueldoResponse> registrar(@Valid @RequestBody SueldoRequest req) {
        return ResponseEntity.status(201).body(service.registrar(req));
    }
}
