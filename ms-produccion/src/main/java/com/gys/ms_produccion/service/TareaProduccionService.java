package com.gys.ms_produccion.service;

import com.gys.ms_produccion.dto.TareaRequest;
import com.gys.ms_produccion.dto.TareaResponse;
import com.gys.ms_produccion.exception.BusinessException;
import com.gys.ms_produccion.exception.ResourceNotFoundException;
import com.gys.ms_produccion.model.EstadoTarea;
import com.gys.ms_produccion.model.TareaProduccion;
import com.gys.ms_produccion.repository.TareaProduccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TareaProduccionService implements ITareaProduccionService {

    private final TareaProduccionRepository repository;

    // Orden de estados en el flujo de producción
    private static final List<EstadoTarea> FLUJO = List.of(
            EstadoTarea.RECIBIDO,
            EstadoTarea.EN_PROCESO,
            EstadoTarea.CONTROL,
            EstadoTarea.LISTO
    );

    @Override
    public List<TareaResponse> listarActivas() {
        return repository.findByActivoTrueOrderByPrioridadDescFechaEntregaEstimadaAsc()
                .stream()
                .map(TareaResponse::from)
                .toList();
    }

    @Override
    public List<TareaResponse> listarPorEstado(EstadoTarea estado) {
        return repository.findByEstadoAndActivoTrueOrderByPrioridadDescFechaEntregaEstimadaAsc(estado)
                .stream()
                .map(TareaResponse::from)
                .toList();
    }

    @Override
    public TareaResponse buscarPorId(Long id) {
        return repository.findById(id)
                .map(TareaResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("TareaProduccion", id));
    }

    @Override
    @Transactional
    public TareaResponse crear(TareaRequest req) {
        TareaProduccion tarea = TareaProduccion.builder()
                .nroPedido(req.nroPedido())
                .paciente(req.paciente())
                .odontologoNombre(req.odontologoNombre())
                .trabajo(req.trabajo())
                .tecnicoNombre(req.tecnicoNombre())
                .prioridad(req.prioridad())
                .estado(EstadoTarea.RECIBIDO)
                .fechaIngreso(req.fechaIngreso())
                .fechaEntregaEstimada(req.fechaEntregaEstimada())
                .observaciones(req.observaciones())
                .build();
        return TareaResponse.from(repository.save(tarea));
    }

    @Override
    @Transactional
    public TareaResponse actualizarEstado(Long id, EstadoTarea nuevoEstado) {
        TareaProduccion tarea = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TareaProduccion", id));

        if (!FLUJO.contains(nuevoEstado)) {
            throw new BusinessException("Estado inválido: " + nuevoEstado);
        }

        tarea.setEstado(nuevoEstado);
        return TareaResponse.from(repository.save(tarea));
    }

    @Override
    @Transactional
    public TareaResponse asignarTecnico(Long id, String tecnico) {
        if (tecnico == null || tecnico.isBlank()) {
            throw new BusinessException("El nombre del técnico no puede estar vacío.");
        }
        TareaProduccion tarea = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TareaProduccion", id));
        tarea.setTecnicoNombre(tecnico);
        return TareaResponse.from(repository.save(tarea));
    }
}
