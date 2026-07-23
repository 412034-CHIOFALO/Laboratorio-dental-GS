package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.DeudaProveedorRequest;
import com.gs.ms_finanzas.dto.DeudaProveedorResponse;
import com.gs.ms_finanzas.exception.BusinessException;
import com.gs.ms_finanzas.exception.ResourceNotFoundException;
import com.gs.ms_finanzas.model.CajaMovimiento;
import com.gs.ms_finanzas.model.DeudaProveedor;
import com.gs.ms_finanzas.model.EstadoDeuda;
import com.gs.ms_finanzas.model.Proveedor;
import com.gs.ms_finanzas.model.TipoCaja;
import com.gs.ms_finanzas.model.TipoMovimientoCaja;
import com.gs.ms_finanzas.repository.CajaMovimientoRepository;
import com.gs.ms_finanzas.repository.DeudaProveedorRepository;
import com.gs.ms_finanzas.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeudaProveedorService implements IDeudaProveedorService {

    private final DeudaProveedorRepository deudaRepo;
    private final ProveedorRepository proveedorRepo;
    private final CajaMovimientoRepository cajaMovimientoRepo;

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

    @Transactional
    public DeudaProveedorResponse pagar(Long id, TipoCaja caja) {
        DeudaProveedor d = deudaRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("DeudaProveedor", id));
        if (d.getEstado() == EstadoDeuda.PAGADO) {
            throw new BusinessException("La deuda ya está marcada como pagada.");
        }
        d.setEstado(EstadoDeuda.PAGADO);
        d.setFechaPago(LocalDate.now());
        DeudaProveedor guardada = deudaRepo.save(d);

        // Antes esto solo cambiaba el estado sin registrar ningún egreso de caja
        // — el pago "desaparecía" contablemente. Ahora sí queda el rastro.
        cajaMovimientoRepo.save(CajaMovimiento.builder()
            .tipo(TipoMovimientoCaja.EGRESO)
            .tipoCaja(caja)
            .monto(d.getMonto())
            .concepto("Pago a proveedor: " + d.getProveedor().getNombre() + " — " + d.getDescripcion())
            .referencia(d.getNroFacturaProveedor())
            .creadoPor("panel")
            .build());

        return DeudaProveedorResponse.from(guardada);
    }
}
