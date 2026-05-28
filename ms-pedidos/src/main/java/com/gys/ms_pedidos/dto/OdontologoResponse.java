package com.gys.ms_pedidos.dto;

import com.gys.ms_pedidos.model.Odontologo;

import java.time.LocalDateTime;

public record OdontologoResponse(
        Long id,
        String nombre,
        String telefono,
        String email,
        String matricula,
        Boolean activo,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaModificacion
) {
    public static OdontologoResponse from(Odontologo o) {
        return new OdontologoResponse(
                o.getId(),
                o.getNombre(),
                o.getTelefono(),
                o.getEmail(),
                o.getMatricula(),
                o.getActivo(),
                o.getFechaCreacion(),
                o.getFechaModificacion()
        );
    }
}
