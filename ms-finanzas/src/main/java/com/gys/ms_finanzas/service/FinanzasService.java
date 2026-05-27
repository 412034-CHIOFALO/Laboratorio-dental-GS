package com.gys.ms_finanzas.service;

import com.gys.ms_finanzas.dto.ComprobanteRequest;
import com.gys.ms_finanzas.dto.ComprobanteResponse;
import com.gys.ms_finanzas.exception.BusinessException;
import com.gys.ms_finanzas.exception.ResourceNotFoundException;
import com.gys.ms_finanzas.model.Comprobante;
import com.gys.ms_finanzas.model.EstadoPago;
import com.gys.ms_finanzas.repository.ComprobanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanzasService implements IFinanzasService {

    private final ComprobanteRepository repository;

    public List<ComprobanteResponse> listarTodos() {
        return repository.findAll().stream().map(ComprobanteResponse::from).toList();
    }

    public List<ComprobanteResponse> listarPorOdontologo(Long odontologoId) {
        return repository.findByOdontologoId(odontologoId)
                .stream().map(ComprobanteResponse::from).toList();
    }

    public List<ComprobanteResponse> listarPendientes() {
        return repository.findByEstadoPago(EstadoPago.PENDIENTE)
                .stream().map(ComprobanteResponse::from).toList();
    }

    public BigDecimal saldoPendienteOdontologo(Long odontologoId) {
        return repository.sumMontosPendientesByOdontologo(odontologoId);
    }

    public ComprobanteResponse buscarPorId(Long id) {
        return repository.findById(id)
                .map(ComprobanteResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante", id));
    }

    @Override
    @Transactional
    public ComprobanteResponse emitir(ComprobanteRequest request) {
        Comprobante c = Comprobante.builder()
                .nroComprobante(generarNroComprobante())
                .pedidoId(request.getPedidoId())
                .nroPedido(request.getNroPedido())
                .odontologoId(request.getOdontologoId())
                .odontologoNombre(request.getOdontologoNombre())
                .trabajo(request.getTrabajo())
                .monto(request.getMonto())
                .fechaEmision(request.getFechaEmision())
                .fechaVencimiento(request.getFechaVencimiento())
                .observaciones(request.getObservaciones())
                .build();
        return ComprobanteResponse.from(repository.save(c));
    }

    @Override
    @Transactional
    public ComprobanteResponse registrarCobro(Long id) {
        Comprobante c = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comprobante", id));
        if (c.getEstadoPago() == EstadoPago.COBRADO) {
            throw new BusinessException("El comprobante ya fue cobrado");
        }
        c.setEstadoPago(EstadoPago.COBRADO);
        c.setFechaCobro(LocalDate.now());
        return ComprobanteResponse.from(repository.save(c));
    }

    private String generarNroComprobante() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long count = repository.count() + 1;
        return String.format("COMP-%s-%04d", fecha, count);
    }
}
