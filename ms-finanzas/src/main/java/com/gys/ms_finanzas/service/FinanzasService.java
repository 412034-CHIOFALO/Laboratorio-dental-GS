package com.gys.ms_finanzas.service;

import com.gys.ms_finanzas.dto.ComprobanteRequest;
import com.gys.ms_finanzas.dto.ComprobanteResponse;
import com.gys.ms_finanzas.dto.CuentaCorrienteOdontologoResponse;
import com.gys.ms_finanzas.dto.CuentaCorrienteOdontologoResponse.Severidad;
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
import java.time.temporal.ChronoUnit;
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

    @Override
    public List<CuentaCorrienteOdontologoResponse> rankingMorosos() {
        List<Object[]> rows = repository.rankingDeudoresRaw();
        LocalDate hoy = LocalDate.now();

        return rows.stream()
                .map(r -> {
                    Long odontologoId        = (Long) r[0];
                    String odontologoNombre  = (String) r[1];
                    BigDecimal totalDeuda    = (BigDecimal) r[2];
                    long comprobantes        = (Long) r[3];
                    LocalDate fechaMasVieja  = (LocalDate) r[4];

                    long diasSinPagar = fechaMasVieja != null
                            ? ChronoUnit.DAYS.between(fechaMasVieja, hoy)
                            : 0;

                    return new CuentaCorrienteOdontologoResponse(
                            odontologoId,
                            odontologoNombre,
                            totalDeuda,
                            comprobantes,
                            fechaMasVieja,
                            diasSinPagar,
                            calcularSeveridad(totalDeuda, diasSinPagar)
                    );
                })
                .toList();
    }

    /**
     * Combina monto + tiempo para clasificar la severidad. Toma el peor de
     * los dos criterios. Los umbrales son configurables más adelante si hace
     * falta (por ahora hardcoded — buen lugar para sacar a properties si el
     * cliente los quiere ajustar).
     */
    private Severidad calcularSeveridad(BigDecimal monto, long diasSinPagar) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return Severidad.AL_DIA;
        }

        // Por monto
        Severidad porMonto;
        if (monto.compareTo(new BigDecimal("500000")) > 0)      porMonto = Severidad.CRITICA;
        else if (monto.compareTo(new BigDecimal("200000")) > 0) porMonto = Severidad.ALTA;
        else if (monto.compareTo(new BigDecimal("50000")) > 0)  porMonto = Severidad.MEDIA;
        else                                                     porMonto = Severidad.BAJA;

        // Por tiempo
        Severidad porTiempo;
        if (diasSinPagar > 90)      porTiempo = Severidad.CRITICA;
        else if (diasSinPagar > 60) porTiempo = Severidad.ALTA;
        else if (diasSinPagar > 30) porTiempo = Severidad.MEDIA;
        else                        porTiempo = Severidad.BAJA;

        // Devuelve la peor
        return porMonto.ordinal() >= porTiempo.ordinal() ? porMonto : porTiempo;
    }

    private String generarNroComprobante() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        long count = repository.count() + 1;
        return String.format("COMP-%s-%04d", fecha, count);
    }
}
