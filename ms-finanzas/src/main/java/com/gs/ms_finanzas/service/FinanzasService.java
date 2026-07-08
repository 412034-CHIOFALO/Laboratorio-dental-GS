package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.ComprobanteRequest;
import com.gs.ms_finanzas.dto.ComprobanteResponse;
import com.gs.ms_finanzas.dto.CuentaCorrienteOdontologoResponse;
import com.gs.ms_finanzas.dto.CuentaCorrienteOdontologoResponse.Severidad;
import com.gs.ms_finanzas.dto.PagoCuentaCorrienteRequest;
import com.gs.ms_finanzas.dto.PagoCuentaCorrienteResponse;
import com.gs.ms_finanzas.exception.BusinessException;
import com.gs.ms_finanzas.exception.ResourceNotFoundException;
import com.gs.ms_finanzas.model.CajaMovimiento;
import com.gs.ms_finanzas.model.Comprobante;
import com.gs.ms_finanzas.model.EstadoPago;
import com.gs.ms_finanzas.model.MedioPago;
import com.gs.ms_finanzas.model.PagoCuentaCorriente;
import com.gs.ms_finanzas.model.TipoCaja;
import com.gs.ms_finanzas.model.TipoMovimientoCaja;
import com.gs.ms_finanzas.repository.CajaMovimientoRepository;
import com.gs.ms_finanzas.repository.ComprobanteRepository;
import com.gs.ms_finanzas.repository.PagoCuentaCorrienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanzasService implements IFinanzasService {

    private final ComprobanteRepository repository;
    private final CajaMovimientoRepository cajaRepo;
    private final PagoCuentaCorrienteRepository pagoRepo;

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
        c.setMontoPagado(c.getMonto());
        c.setFechaCobro(LocalDate.now());
        return ComprobanteResponse.from(repository.save(c));
    }

    /**
     * Registra un pago manual a la cuenta corriente de un odontólogo. El monto se
     * imputa a sus comprobantes con saldo (más viejos primero), pudiendo cubrir
     * parcial o totalmente varios. El dinero ingresa a la caja según el medio
     * (efectivo → Física, transferencia → Bancaria). No genera saldo a favor: si
     * el pago supera la deuda, solo se imputa/ingresa lo aplicable.
     */
    @Override
    @Transactional
    public PagoCuentaCorrienteResponse registrarPagoCuentaCorriente(Long odontologoId, PagoCuentaCorrienteRequest req) {
        List<Comprobante> conSaldo = repository
                .findByOdontologoIdAndEstadoPagoIn(odontologoId, List.of(EstadoPago.PENDIENTE, EstadoPago.PARCIAL))
                .stream()
                .sorted(Comparator.comparing(Comprobante::getFechaEmision))
                .toList();

        if (conSaldo.isEmpty()) {
            throw new BusinessException("El odontólogo no tiene deudas pendientes para imputar el pago.");
        }

        String nombre = conSaldo.get(0).getOdontologoNombre();
        LocalDate fecha = req.fecha() != null ? req.fecha() : LocalDate.now();
        BigDecimal restante = req.monto();
        BigDecimal imputado = BigDecimal.ZERO;
        int afectados = 0;

        for (Comprobante c : conSaldo) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal saldo = c.getSaldoPendiente();
            if (saldo.compareTo(BigDecimal.ZERO) <= 0) continue;
            BigDecimal aplica = restante.min(saldo);

            c.setMontoPagado(c.getMontoPagado().add(aplica));
            if (c.getMontoPagado().compareTo(c.getMonto()) >= 0) {
                c.setEstadoPago(EstadoPago.COBRADO);
                c.setFechaCobro(fecha);
            } else {
                c.setEstadoPago(EstadoPago.PARCIAL);
            }
            repository.save(c);

            restante = restante.subtract(aplica);
            imputado = imputado.add(aplica);
            afectados++;
        }

        // Ingreso de caja por lo efectivamente imputado (sin saldo a favor).
        TipoCaja caja = req.medio() == MedioPago.TRANSFERENCIA ? TipoCaja.BANCARIA : TipoCaja.FISICA;
        if (imputado.compareTo(BigDecimal.ZERO) > 0) {
            cajaRepo.save(CajaMovimiento.builder()
                    .tipo(TipoMovimientoCaja.INGRESO)
                    .tipoCaja(caja)
                    .concepto("Cobro cuenta corriente: " + nombre)
                    .monto(imputado)
                    .creadoPor("panel")
                    .build());
        }

        PagoCuentaCorriente pago = pagoRepo.save(PagoCuentaCorriente.builder()
                .odontologoId(odontologoId)
                .odontologoNombre(nombre)
                .monto(req.monto())
                .montoImputado(imputado)
                .medio(req.medio())
                .fecha(fecha)
                .nota(req.nota())
                .build());

        BigDecimal saldoRestante = repository.sumMontosPendientesByOdontologo(odontologoId);
        String mensaje = "Pago imputado a " + afectados + " comprobante(s). Saldo restante: $" + saldoRestante;
        if (restante.compareTo(BigDecimal.ZERO) > 0) {
            mensaje += ". Excedente no imputado (sin saldo a favor): $" + restante;
        }

        return new PagoCuentaCorrienteResponse(
                pago.getId(), odontologoId, nombre,
                req.monto(), imputado, req.medio(), fecha, req.nota(),
                afectados, saldoRestante, mensaje
        );
    }

    @Override
    public List<PagoCuentaCorrienteResponse> historialPagosOdontologo(Long odontologoId) {
        return pagoRepo.findByOdontologoIdOrderByFechaDescIdDesc(odontologoId)
                .stream().map(PagoCuentaCorrienteResponse::from).toList();
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
