package com.gys.ms_finanzas.service;

import com.gys.ms_finanzas.dto.DeudaProveedorRequest;
import com.gys.ms_finanzas.dto.DeudaProveedorResponse;
import com.gys.ms_finanzas.exception.ResourceNotFoundException;
import com.gys.ms_finanzas.model.DeudaProveedor;
import com.gys.ms_finanzas.model.EstadoDeuda;
import com.gys.ms_finanzas.model.Proveedor;
import com.gys.ms_finanzas.repository.DeudaProveedorRepository;
import com.gys.ms_finanzas.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeudaProveedorService implements IDeudaProveedorService {

    private final DeudaProveedorRepository deudaRepo;
    private final ProveedorRepository proveedorRepo;

    public List<DeudaProveedorResponse> listarPorProveedor(Long proveedorId) {
        return deudaRepo.findByProveedorIdOrderByFechaCreacionDesc(proveedorId).stream()
            .map(DeudaProveedorResponse::from)
            .toList();
    }

    public List<DeudaProveedorResponse> listarPendientes() {
        return deudaRepo.findByEstadoOrderByFechaVencimientoAsc(EstadoDeuda.PENDIENTE).stream()
            .map(DeudaProveedorResponse::from)
            .toList();
    }

    public DeudaProveedorResponse buscarPorId(Long id) {
        return deudaRepo.findById(id)
            .map(DeudaProveedorResponse::from)
            .orElseThrow(() -> new ResourceNotFoundException("DeudaProveedor", id));
    }

    @Transactional
    public DeudaProveedorResponse registrar(DeudaProveedorRequest request) {
        Proveedor proveedor = proveedorRepo.findById(request.getProveedorId())
            .orElseThrow(() -> new ResourceNotFoundException("Proveedor", request.getProveedorId()));
        DeudaProveedor d = DeudaProveedor.builder()
            .proveedor(proveedor)
            .descripcion(request.getDescripcion())
            .monto(request.getMonto())
            .fechaVencimiento(request.getFechaVencimiento())
            .nroFacturaProveedor(request.getNroFacturaProveedor())
            .observaciones(request.getObservaciones())
            .build();
        return DeudaProveedorResponse.from(deudaRepo.save(d));
    }
}
