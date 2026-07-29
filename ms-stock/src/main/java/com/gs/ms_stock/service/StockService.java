package com.gs.ms_stock.service;

import com.gs.ms_stock.dto.MaterialRequest;
import com.gs.ms_stock.dto.MaterialResponse;
import com.gs.ms_stock.dto.MovimientoRequest;
import com.gs.ms_stock.exception.ResourceNotFoundException;
import com.gs.ms_stock.model.Material;
import com.gs.ms_stock.model.MovimientoStock;
import com.gs.ms_stock.repository.MaterialRepository;
import com.gs.ms_stock.repository.MovimientoStockRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementación de {@link IStockService} para la gestión del inventario de materiales.
 *
 * <p>Administra el ciclo de vida de los materiales del laboratorio y sus movimientos:</p>
 * <ul>
 *   <li>CRUD de materiales con baja lógica (soft delete via {@code activo = false}).</li>
 *   <li>Registro de movimientos: {@code ENTRADA}, {@code SALIDA} y {@code AJUSTE}.</li>
 *   <li>Política permisiva: permite stock negativo con advertencia en log en vez de
 *       lanzar excepción, reflejando la realidad operativa del laboratorio.</li>
 * </ul>
 *
 * <p>Las salidas automáticas por producción son iniciadas por ms-pedidos a través
 * del endpoint {@code POST /api/stock/movimiento}.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService implements IStockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final MaterialRepository materialRepo;
    private final MovimientoStockRepository movimientoRepo;
    private final AuditoriaClient auditoria;

    public List<MaterialResponse> listarActivos() {
        return materialRepo.findByActivoTrue().stream().map(MaterialResponse::from).toList();
    }

    public List<MaterialResponse> listarBajoStock() {
        return materialRepo.findBajoStock().stream().map(MaterialResponse::from).toList();
    }

    public MaterialResponse buscarPorId(Long id) {
        return materialRepo.findById(id)
                .map(MaterialResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Material", id));
    }

    @Override
    @Transactional
    public MaterialResponse crear(MaterialRequest request) {
        Material m = Material.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .categoria(request.getCategoria())
                .stockActual(request.getStockActual())
                .stockMinimo(request.getStockMinimo())
                .unidadMedida(request.getUnidadMedida())
                .precioUnitario(request.getPrecioUnitario())
                .proveedor(request.getProveedor())
                .descuentaStock(request.getDescuentaStock() == null || request.getDescuentaStock())
                .build();
        MaterialResponse resp = MaterialResponse.from(materialRepo.save(m));
        auditoria.registrar("CREAR", "Material creado", "Material " + m.getNombre(),
                "Stock inicial " + m.getStockActual() + " " + m.getUnidadMedida());
        return resp;
    }

    @Override
    @Transactional
    public MaterialResponse actualizar(Long id, MaterialRequest request) {
        Material m = materialRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material", id));
        m.setNombre(request.getNombre());
        m.setDescripcion(request.getDescripcion());
        m.setCategoria(request.getCategoria());
        m.setStockActual(request.getStockActual());
        m.setStockMinimo(request.getStockMinimo());
        m.setUnidadMedida(request.getUnidadMedida());
        m.setPrecioUnitario(request.getPrecioUnitario());
        m.setProveedor(request.getProveedor());
        if (request.getDescuentaStock() != null) {
            m.setDescuentaStock(request.getDescuentaStock());
        }
        return MaterialResponse.from(materialRepo.save(m));
    }

    @Override
    @Transactional
    public MaterialResponse registrarMovimiento(MovimientoRequest request) {
        Material material = resolverMaterial(request);

        // Política nueva: avisamos pero permitimos descontar a negativo
        // (refleja la realidad — el admin sabe que está "debiendo" material)
        double nuevoStock = calcularNuevoStock(material.getStockActual(), request);
        if (nuevoStock < 0) {
            log.warn("[GS-STOCK] Stock negativo al registrar movimiento {} para material '{}': {} {} disponibles, se descontaron {} → resultado {}",
                request.getTipo(), material.getNombre(),
                material.getStockActual(), material.getUnidadMedida(),
                request.getCantidad(), nuevoStock);
        }

        material.setStockActual(nuevoStock);
        materialRepo.save(material);

        MovimientoStock mov = MovimientoStock.builder()
                .material(material)
                .tipo(request.getTipo())
                .cantidad(request.getCantidad())
                .stockResultante(nuevoStock)
                .motivo(request.getMotivo())
                .pedidoId(request.getPedidoId())
                .build();
        movimientoRepo.save(mov);

        auditoria.registrar("STOCK", "Movimiento de stock", "Material " + material.getNombre(),
                request.getTipo() + " " + request.getCantidad() + " " + material.getUnidadMedida()
                        + " → " + nuevoStock + (request.getMotivo() != null ? " · " + request.getMotivo() : ""));

        return MaterialResponse.from(material);
    }

    /**
     * Resuelve el material del movimiento priorizando el nombre sobre el id.
     *
     * Quien más depende de esto es ms-pedidos: cuando descuenta stock según la
     * receta del catálogo, el materialId que maneja viene de un seed hardcodeado
     * en ms-catalogo (no hay acceso cruzado entre bases de datos de microservicios
     * para resolverlo en el momento en que se creó esa receta). Si ese id quedó
     * mal — por ejemplo porque la tabla de materiales ya tenía filas antes de
     * correr el seed — resolver por nombre evita que el descuento falle en
     * silencio contra un material inexistente o equivocado.
     */
    private Material resolverMaterial(MovimientoRequest request) {
        String nombre = request.getMaterialNombre();
        if (nombre != null && !nombre.isBlank()) {
            Optional<Material> porNombre = materialRepo.findByNombreIgnoreCase(nombre.trim());
            if (porNombre.isPresent()) {
                return porNombre.get();
            }
            log.warn("[GS-STOCK] No hay material con nombre '{}' — se intenta resolver por id={}.",
                nombre, request.getMaterialId());
        }
        return materialRepo.findById(request.getMaterialId())
                .orElseThrow(() -> new ResourceNotFoundException("Material", request.getMaterialId()));
    }

    @Transactional
    public void eliminar(Long id) {
        Material m = materialRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material", id));
        m.setActivo(false);
        materialRepo.save(m);
    }

    private double calcularNuevoStock(double actual, MovimientoRequest req) {
        return switch (req.getTipo()) {
            case ENTRADA -> actual + req.getCantidad();
            case SALIDA  -> actual - req.getCantidad();
            case AJUSTE  -> req.getCantidad(); // reemplaza
        };
    }
}
