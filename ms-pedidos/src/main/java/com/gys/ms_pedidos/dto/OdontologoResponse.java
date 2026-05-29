package com.gys.ms_pedidos.dto;

import com.gys.ms_pedidos.model.Odontologo;

import java.time.LocalDateTime;

public record OdontologoResponse(
        Long id,
        String nombre,
        String dni,
        String cuit,
        String telefono,
        String email,
        String matricula,
        String clinica,
        String direccion,
        Boolean activo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaModificacion
) {
    public static OdontologoResponse from(Odontologo o) {
        return new OdontologoResponse(
                o.getId(),
                o.getNombre(),
                o.getDni(),
                o.getCuit(),
                o.getTelefono(),
                o.getEmail(),
                o.getMatricula(),
                o.getClinica(),
                o.getDireccion(),
                o.getActivo(),
                o.getFechaCreacion(),
                o.getFechaModificacion()
        );
    }
}
