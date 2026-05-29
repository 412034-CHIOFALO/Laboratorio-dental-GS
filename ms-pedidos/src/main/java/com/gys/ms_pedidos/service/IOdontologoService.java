package com.gys.ms_pedidos.service;

import com.gys.ms_pedidos.dto.OdontologoRequest;
import com.gys.ms_pedidos.dto.OdontologoResponse;
import com.gys.ms_pedidos.model.Odontologo;

import java.util.List;

public interface IOdontologoService {

    /** Listado completo de activos, ordenados alfabéticamente. */
    List<OdontologoResponse> listarActivos();

    /** Autocomplete: trae los odontólogos cuyo nombre contenga el fragmento. */
    List<OdontologoResponse> buscarPorNombre(String fragmento);

    OdontologoResponse buscarPorId(Long id);

    OdontologoResponse crear(OdontologoRequest request);

    OdontologoResponse actualizar(Long id, OdontologoRequest request);

    /** Desactiva (soft delete) — no se borra para preservar referencias en pedidos. */
    void desactivar(Long id);

    /**
     * Patrón "find or create": busca por nombre exacto (case-insensitive).
     * Si no existe, lo crea con ese nombre y sin datos de contacto.
     * Es el método usado por el flujo "Nuevo pedido".
     */
    Odontologo buscarOCrearPorNombre(String nombre);
}
