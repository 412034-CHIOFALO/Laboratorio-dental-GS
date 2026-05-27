package com.gys.ms_stock.service;

import com.gys.ms_stock.dto.MaterialRequest;
import com.gys.ms_stock.dto.MaterialResponse;
import com.gys.ms_stock.dto.MovimientoRequest;
import com.gys.ms_stock.exception.BusinessException;
import com.gys.ms_stock.exception.ResourceNotFoundException;
import com.gys.ms_stock.model.Material;
import com.gys.ms_stock.model.MovimientoStock;
import com.gys.ms_stock.model.TipoMovimiento;
import com.gys.ms_stock.repository.MaterialRepository;
import com.gys.ms_stock.repository.MovimientoStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService implements IStockService {

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
                .build();
        return MaterialResponse.from(materialRepo.save(m));
    }

    @Transactional
    public MaterialResponse registrarMovimiento(MovimientoRequest request) {
        Material material = materialRepo.findById(request.getMaterialId())
                .orElseThrow(() -> new ResourceNotFoundException("Material", request.getMaterialId()));

        double nuevoStock = calcularNuevoStock(material.getStockActual(), request);
        if (nuevoStock < 0) {
            throw new BusinessException("Stock insuficiente. Disponible: " + material.getStockActual()
                            + " " + material.getUnidadMedida());
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
