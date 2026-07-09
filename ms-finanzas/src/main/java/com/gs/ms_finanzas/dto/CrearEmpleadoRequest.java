package com.gs.ms_finanzas.dto;

import com.gs.ms_finanzas.model.FrecuenciaPago;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Alta manual de un integrante del laboratorio en el módulo de sueldos.
 * <p>
 * ms-finanzas mantiene su propia tabla de empleados (denormalizada de
 * ms-auth), por lo que un usuario nuevo creado en Usuarios no aparece acá
 * ni es reconocido por el bot hasta que se lo da de alta con este endpoint.
 * </p>
 */
@Data
public class CrearEmpleadoRequest {

    @NotNull(message = "El ID de usuario (ms-auth) es obligatorio")
    private Long usuarioId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    @Size(max = 30, message = "El rol no puede superar los 30 caracteres")
    private String rol;

    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
    private String telefono;

    @NotNull(message = "La frecuencia es obligatoria")
    private FrecuenciaPago frecuencia;

    @NotNull(message = "El monto base es obligatorio")
    @PositiveOrZero(message = "El monto base no puede ser negativo")
    @Digits(integer = 10, fraction = 2, message = "El monto base excede el máximo permitido")
    private BigDecimal montoBase;
}
