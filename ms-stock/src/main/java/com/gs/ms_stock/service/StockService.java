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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService implements IStockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final MaterialRepository materialRepo;
    private final MovimientoStockRepository movimientoRepo;

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
        return MaterialResponse.from(materialRepo.save(m));
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
        Material material = materialRepo.findById(request.getMaterialId())
                .orElseThrow(() -> new ResourceNotFoundException("Material", request.getMaterialId()));

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

        return MaterialResponse.from(material);
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
