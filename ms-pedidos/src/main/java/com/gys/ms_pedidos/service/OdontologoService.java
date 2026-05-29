package com.gys.ms_pedidos.service;

import com.gys.ms_pedidos.dto.OdontologoRequest;
import com.gys.ms_pedidos.dto.OdontologoResponse;
import com.gys.ms_pedidos.exception.BusinessException;
import com.gys.ms_pedidos.exception.ConflictException;
import com.gys.ms_pedidos.exception.ResourceNotFoundException;
import com.gys.ms_pedidos.model.Odontologo;
import com.gys.ms_pedidos.repository.OdontologoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OdontologoService implements IOdontologoService {

    private static final Logger log = LoggerFactory.getLogger(OdontologoService.class);

    // Patrones para detección automática del tipo de búsqueda
    private static final Pattern PATRON_DNI = Pattern.compile("^[0-9]{7,8}$");
    private static final Pattern PATRON_CUIT_RAW = Pattern.compile("^[0-9]{2}-?[0-9]{8}-?[0-9]{1}$");
    private static final Pattern PATRON_MATRICULA = Pattern.compile("^(MN|MP|MAT)[\\s-]*[0-9]+$", Pattern.CASE_INSENSITIVE);

    private final OdontologoRepository repository;

    @Override
    public List<OdontologoResponse> listarActivos() {
        return repository.findByActivoTrueOrderByNombreAsc()
                .stream()
                .map(OdontologoResponse::from)
                .toList();
    }

    /**
     * Búsqueda inteligente: detecta automáticamente el tipo según el formato del input.
     *
     * - "12345678"     → DNI exacto
     * - "20-12345678-9"→ CUIT exacto (también acepta sin guiones)
     * - "MN 12345"     → Matrícula exacta
     * - "garcia"       → Fragmento de nombre
     *
     * Si el match exacto encuentra al odontólogo, devuelve solo ese. Si no, busca por nombre.
     */
    @Override
    public List<OdontologoResponse> buscarPorNombre(String fragmento) {
        if (fragmento == null || fragmento.isBlank()) {
            return listarActivos();
        }
        String q = fragmento.trim();

        // Match exacto por DNI
        if (PATRON_DNI.matcher(q).matches()) {
            return repository.findByActivoTrueAndDni(q)
                    .map(o -> List.of(OdontologoResponse.from(o)))
                    .orElseGet(List::of);
        }
        // Match exacto por CUIT (normalizando guiones)
        if (PATRON_CUIT_RAW.matcher(q).matches()) {
            String cuitNormalizado = normalizarCuit(q);
            return repository.findByActivoTrueAndCuit(cuitNormalizado)
                    .map(o -> List.of(OdontologoResponse.from(o)))
                    .orElseGet(List::of);
        }
        // Match exacto por matrícula
        if (PATRON_MATRICULA.matcher(q).matches()) {
            return repository.findByActivoTrueAndMatriculaIgnoreCase(q)
                    .map(o -> List.of(OdontologoResponse.from(o)))
                    .orElseGet(List::of);
        }

        // Default: búsqueda por nombre (incluye también matches parciales en matrícula
        // para usuarios que escriben "12345" sin el prefijo MN)
        Set<Odontologo> resultados = new LinkedHashSet<>(
                repository.findByActivoTrueAndNombreContainingIgnoreCaseOrderByNombreAsc(q));
        return resultados.stream().map(OdontologoResponse::from).toList();
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
        String nombre = normalizarNombre(request.getNombre());
        String dni    = blankToNull(request.getDni());
        String cuit   = normalizarCuit(blankToNull(request.getCuit()));

        // Validación de unicidad
        repository.findByActivoTrueAndNombreIgnoreCase(nombre).ifPresent(existente -> {
            throw new ConflictException("Ya existe un odontólogo con nombre: " + existente.getNombre());
        });
        if (dni != null && repository.existsByDni(dni)) {
            throw new ConflictException("Ya existe un odontólogo con DNI: " + dni);
        }
        if (cuit != null && repository.existsByCuit(cuit)) {
            throw new ConflictException("Ya existe un odontólogo con CUIT: " + cuit);
        }

        Odontologo nuevo = Odontologo.builder()
                .nombre(nombre)
                .dni(dni)
                .cuit(cuit)
                .telefono(blankToNull(request.getTelefono()))
                .email(blankToNull(request.getEmail()))
                .matricula(blankToNull(request.getMatricula()))
                .clinica(blankToNull(request.getClinica()))
                .direccion(blankToNull(request.getDireccion()))
                .build();
        return OdontologoResponse.from(repository.save(nuevo));
    }

    @Override
    @Transactional
    public OdontologoResponse actualizar(Long id, OdontologoRequest request) {
        Odontologo o = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Odontologo", id));

        String dni  = blankToNull(request.getDni());
        String cuit = normalizarCuit(blankToNull(request.getCuit()));

        // Si cambia el DNI/CUIT, validar que no choque con otro odontólogo
        if (dni != null && !dni.equals(o.getDni()) && repository.existsByDni(dni)) {
            throw new ConflictException("Ya existe otro odontólogo con DNI: " + dni);
        }
        if (cuit != null && !cuit.equals(o.getCuit()) && repository.existsByCuit(cuit)) {
            throw new ConflictException("Ya existe otro odontólogo con CUIT: " + cuit);
        }

        o.setNombre(normalizarNombre(request.getNombre()));
        o.setDni(dni);
        o.setCuit(cuit);
        o.setTelefono(blankToNull(request.getTelefono()));
        o.setEmail(blankToNull(request.getEmail()));
        o.setMatricula(blankToNull(request.getMatricula()));
        o.setClinica(blankToNull(request.getClinica()));
        o.setDireccion(blankToNull(request.getDireccion()));
        return OdontologoResponse.from(repository.save(o));
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Odontologo o = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Odontologo", id));
        o.setActivo(false);
        repository.save(o);
        log.info("[GYS-PEDIDOS] Odontólogo desactivado: {} (id={})", o.getNombre(), id);
    }

    @Override
    @Transactional
    public Odontologo buscarOCrearPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new BusinessException("El nombre del odontólogo es obligatorio.");
        }
        String normalizado = normalizarNombre(nombre);
        return repository.findByActivoTrueAndNombreIgnoreCase(normalizado)
                .orElseGet(() -> {
                    log.info("[GYS-PEDIDOS] Odontólogo nuevo creado on-the-fly: {}", normalizado);
                    Odontologo nuevo = Odontologo.builder().nombre(normalizado).build();
                    return repository.save(nuevo);
                });
    }

    // ── Helpers de normalización ─────────────────────────────────────

    private String normalizarNombre(String s) {
        return s == null ? null : s.trim().replaceAll("\\s+", " ");
    }

    /** Convierte cualquier formato de CUIT a XX-XXXXXXXX-X. */
    private String normalizarCuit(String cuit) {
        if (cuit == null || cuit.isBlank()) return null;
        String digitos = cuit.replaceAll("[^0-9]", "");
        if (digitos.length() != 11) return cuit; // dejar como viene si no son 11 dígitos
        return digitos.substring(0, 2) + "-" + digitos.substring(2, 10) + "-" + digitos.substring(10);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
