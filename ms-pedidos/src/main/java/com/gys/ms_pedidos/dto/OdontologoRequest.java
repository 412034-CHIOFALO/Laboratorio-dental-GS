package com.gys.ms_pedidos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OdontologoRequest {

    @NotBlank(message = "El nombre del odontólogo es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    /** DNI: solo dígitos, 7 u 8 caracteres. */
    @Pattern(regexp = "^[0-9]{7,8}$|^$", message = "El DNI debe tener 7 u 8 dígitos")
    private String dni;

    /** CUIT formato XX-XXXXXXXX-X o XXXXXXXXXXX (sin guiones). */
    @Pattern(regexp = "^[0-9]{2}-?[0-9]{8}-?[0-9]{1}$|^$", message = "El CUIT no tiene un formato válido")
    private String cuit;

    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
    private String telefono;

    @Email(message = "El email no es válido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    private String email;

    @Size(max = 30, message = "La matrícula no puede superar los 30 caracteres")
    private String matricula;
}
