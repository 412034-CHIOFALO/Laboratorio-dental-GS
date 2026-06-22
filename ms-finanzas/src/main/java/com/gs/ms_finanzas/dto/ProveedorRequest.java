package com.gs.ms_finanzas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProveedorRequest {

    @NotBlank
    @Size(max = 200)
    private String nombre;

    @Size(max = 20)
    private String cuit;

    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String telefono;

    @Size(max = 300)
    private String direccion;
}
