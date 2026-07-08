package com.gs.ms_pedidos.client.dto;

import java.util.List;

/**
 * Versión recortada del TipoTrabajoResponse de ms-catalogo.
 * Solo deserializamos lo que necesita ms-pedidos para el descuento de stock.
 *
 * Spring/Jackson ignora los campos que no están en el record (no falla).
 */
public record TipoTrabajoDTO(
        Long id,
        String nombre,
        List<IngredienteRecetaDTO> receta
) { }
