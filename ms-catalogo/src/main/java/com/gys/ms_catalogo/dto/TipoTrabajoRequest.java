package com.gys.ms_catalogo.dto;

import com.gys.ms_catalogo.model.Categoria;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class TipoTrabajoRequest {

    @NotBlank(message = "El nombre del trabajo es obligatorio")
    private String nombre;

    private String descripcion;

    @PositiveOrZero(message = "El precio no puede ser negativo")
    private BigDecimal precio;

    @NotNull(message = "La categoría es obligatoria")
    private Categoria categoria;

    @PositiveOrZero
    private Integer tiempoEstimadoDias = 0;

    private String fotoUrl;

    /** Receta: materiales que consume este trabajo. Opcional, puede venir vacía. */
    @Valid
    private List<IngredienteRecetaRequest> receta = new ArrayList<>();
}
