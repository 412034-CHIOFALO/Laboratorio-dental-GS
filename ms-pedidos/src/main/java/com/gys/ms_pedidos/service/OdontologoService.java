package com.gys.ms_pedidos.service;

import com.gys.ms_pedidos.dto.OdontologoRequest;
import com.gys.ms_pedidos.dto.OdontologoResponse;
import com.gys.ms_pedidos.exception.BusinessException;
import com.gys.ms_pedidos.exception.ResourceNotFoundException;
import com.gys.ms_pedidos.model.Odontologo;
import com.gys.ms_pedidos.repository.OdontologoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OdontologoService implements IOdontologoService {

    private static final Logger log = LoggerFactory.getLogger(OdontologoService.class);

    private final OdontologoRepository repository;

    @Override
    public List<OdontologoResponse> listarActivos() {
        return repository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(OdontologoResponse::from)
                .toList();
    }

    @Override
    public List<OdontologoResponse> buscarPorNombre(String fragmento) {
        if (fragmento == null || fragmento.isBlank()) {
            return listarActivos();
        }
        return repository.findByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(fragmento.trim())
                .stream()
                .map(OdontologoResponse::from)
                .toList();
    }

    @Override
    public OdontologoResponse buscarPorId(Long id) {
        return repository.findById(id)
                .map(OdontologoResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Odontologo", id));
    }

    @Override
    @Transactional
    public OdontologoResponse crear(OdontologoRequest request) {
        String nombre = normalizar(request.getNombre());
        repository.findByActivoTrueAndNombreIgnoreCase(nombre).ifPresent(existente -> {
            throw new BusinessException("Ya existe un odontólogo con nombre: " + existente.getNombre());
        });
        Odontologo nuevo = Odontologo.builder()
                .nombre(nombre)
                .telefono(blankToNull(request.getTelefono()))
                .email(blankToNull(request.getEmail()))
                .matricula(blankToNull(request.getMatricula()))
                .build();
        return OdontologoResponse.from(repository.save(nuevo));
    }

    @Override
    @Transactional
    public OdontologoResponse actualizar(Long id, OdontologoRequest request) {
        Odontologo o = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Odontologo", id));
        o.setNombre(normalizar(request.getNombre()));
        o.setTelefono(blankToNull(request.getTelefono()));
        o.setEmail(blankToNull(request.getEmail()));
        o.setMatricula(blankToNull(request.getMatricula()));
        return OdontologoResponse.from(repository.save(o));
    }

    /**
     * Find-or-create: usado por el flujo de creación de pedidos.
     * Devuelve la entidad (no DTO) porque PedidoService necesita id + nombre.
     */
    @Override
    @Transactional
    public Odontologo buscarOCrearPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new BusinessException("El nombre del odontólogo es obligatorio.");
        }
        String normalizado = normalizar(nombre);
        return repository.findByActivoTrueAndNombreIgnoreCase(normalizado)
                .orElseGet(() -> {
                    log.info("[GYS-PEDIDOS] Odontólogo nuevo creado on-the-fly: {}", normalizado);
                    Odontologo nuevo = Odontologo.builder().nombre(normalizado).build();
                    return repository.save(nuevo);
                });
    }

    private String normalizar(String s) {
        return s == null ? null : s.trim().replaceAll("\\s+", " ");
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
