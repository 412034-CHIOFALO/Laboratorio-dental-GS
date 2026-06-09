package com.gys.ms_catalogo.service;

import com.gys.ms_catalogo.dto.IngredienteRecetaRequest;
import com.gys.ms_catalogo.dto.TipoTrabajoRequest;
import com.gys.ms_catalogo.dto.TipoTrabajoResponse;
import com.gys.ms_catalogo.exception.ResourceNotFoundException;
import com.gys.ms_catalogo.model.Categoria;
import com.gys.ms_catalogo.model.IngredienteReceta;
import com.gys.ms_catalogo.model.TipoTrabajo;
import com.gys.ms_catalogo.repository.TipoTrabajoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TipoTrabajoService implements ITipoTrabajoService {

    private final TipoTrabajoRepository repository;

    public List<TipoTrabajoResponse> listarActivos() {
        return repository.findByActivoTrue()
                .stream()
                .map(TipoTrabajoResponse::from)
                .toList();
    }

    public List<TipoTrabajoResponse> listarPorCategoria(Categoria categoria) {
        return repository.findByCategoriaAndActivoTrue(categoria)
                .stream()
                .map(TipoTrabajoResponse::from)
                .toList();
    }

    public List<TipoTrabajoResponse> buscarPorNombre(String nombre) {
        return repository.findByNombreContainingIgnoreCaseAndActivoTrue(nombre)
                .stream()
                .map(TipoTrabajoResponse::from)
                .toList();
    }

    public TipoTrabajoResponse buscarPorId(Long id) {
        return repository.findById(id)
                .map(TipoTrabajoResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("TipoTrabajo", id));
    }

    @Transactional
    public TipoTrabajoResponse crear(TipoTrabajoRequest request) {
        TipoTrabajo t = TipoTrabajo.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .categoria(request.getCategoria())
                .tiempoEstimadoDias(request.getTiempoEstimadoDias())
                .fotoUrl(request.getFotoUrl())
                .build();
        t.reemplazarReceta(toIngredientes(request));
        return TipoTrabajoResponse.from(repository.save(t));
    }

    @Transactional
    public TipoTrabajoResponse actualizar(Long id, TipoTrabajoRequest request) {
        TipoTrabajo t = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoTrabajo", id));

        t.setNombre(request.getNombre());
        t.setDescripcion(request.getDescripcion());
        t.setPrecio(request.getPrecio());
        t.setCategoria(request.getCategoria());
        t.setTiempoEstimadoDias(request.getTiempoEstimadoDias());
        t.setFotoUrl(request.getFotoUrl());
        t.reemplazarReceta(toIngredientes(request));

        return TipoTrabajoResponse.from(repository.save(t));
    }

    /** Convierte la lista de DTOs en entidades (sin setearles el TipoTrabajo dueño todavía). */
    private java.util.List<IngredienteReceta> toIngredientes(TipoTrabajoRequest request) {
        if (request.getReceta() == null) return java.util.List.of();
        return request.getReceta().stream()
                .map(r -> IngredienteReceta.builder()
                        .materialId(r.getMaterialId())
                        .materialNombre(r.getMaterialNombre())
                        .cantidad(r.getCantidad())
                        .unidad(r.getUnidad())
                        .notas(r.getNotas())
                        .build())
                .toList();
    }

    @Transactional
    public void eliminar(Long id) {
        TipoTrabajo t = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TipoTrabajo", id));
        // Soft delete — no borramos físicamente para no romper referencias en pedidos
        t.setActivo(false);
        repository.save(t);
    }
}
