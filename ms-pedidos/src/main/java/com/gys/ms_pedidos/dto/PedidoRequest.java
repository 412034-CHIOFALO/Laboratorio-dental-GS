package com.gys.ms_pedidos.dto;

import com.gys.ms_pedidos.model.Prioridad;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PedidoRequest {

    /**
     * Opcional. Si viene seteado se usa directamente; si viene null se busca o
     * crea un odontólogo a partir de {@link #odontologoNombre}.
     */
    private Long odontologoId;

    @NotBlank(message = "El nombre del odontólogo es obligatorio")
    private String odontologoNombre;

    @NotBlank(message = "El paciente es obligatorio")
    private String paciente;

    private Long catalogoTrabajoId;

    @NotBlank(message = "El trabajo es obligatorio")
    private String trabajo;

    private Long tecnicoId;
    private String tecnicoNombre;

    @NotNull(message = "La fecha de entrega es obligatoria")
    @Future(message = "La fecha de entrega debe ser futura")
    private LocalDate fechaEntrega;

    private Prioridad prioridad = Prioridad.NORMAL;

    private BigDecimal precioAcordado;
    private String observaciones;
}
