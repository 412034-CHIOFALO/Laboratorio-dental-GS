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
import java.util.Optional;

/**
 * Implementación de {@link IFinanzasService} para la gestión de comprobantes y cuentas corrientes.
 *
 * <p>Un comprobante representa la deuda de un odontólogo por un trabajo entregado.
 * El ciclo de vida del comprobante es:</p>
 * <pre>
 *   PENDIENTE → PARCIAL (pago parcial) → COBRADO (pago total)
 *            ↘ VENCIDO (si supera la fecha de vencimiento sin pagarse)
 * </pre>
 *
 * <p>Los pagos se imputan con la política FIFO (más antiguo primero) a través de
 * {@link #registrarPagoCuentaCorriente(Long, com.gs.ms_finanzas.dto.PagoCuentaCorrienteRequest)}.
 * El dinero resultante se registra en la caja correspondiente al medio de pago.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanzasService implements IFinanzasService {

    private final ComprobanteRepository repository;
    private final CajaMovimientoRepository cajaRepo;
    private final PagoCuentaCorrienteRepository pagoRepo;
    private final AuditoriaClient auditoria;

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
        // Idempotente por pedido: si ya se emitió la deuda de este pedido, se
        // devuelve la existente en vez de crear otra. ms-pedidos emite en modo
        // best-effort y puede reintentar (ej: si la primera vez falló DESPUÉS de
        // guardar); sin esta guarda, el reintento duplicaba la deuda del
        // odontólogo, que es el peor error posible de este flujo.
        Optional<Comprobante> yaEmitido = repository.findByPedidoId(request.getPedidoId());
        if (yaEmitido.isPresent()) {
            return ComprobanteResponse.from(yaEmitido.get());
        }

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
        ComprobanteResponse resp = ComprobanteResponse.from(repository.save(c));
        auditoria.registrar("COBRO", "Comprobante cobrado", "Comprobante " + c.getNroComprobante(),
                "Odontólogo " + c.getOdontologoNombre() + " · $" + c.getMonto());
        return resp;
    }

    /**
     * Sincroniza el monto del comprobante cuando se corrige el precio de un
     * pedido YA entregado (ms-pedidos → PedidoService.actualizar). Sin esto, el
     * comprobante queda congelado con el monto de la entrega original: editar
     * "monto a facturar" después nunca se reflejaba en el saldo pendiente del
     * odontólogo, porque esa edición solo tocaba el Pedido, nunca la deuda real.
     *
     * <p>No permite bajar el monto por debajo de lo ya cobrado (no tiene sentido
     * contable — habría que usar un reintegro, no editar el comprobante).</p>
     *
     * <p>Best-effort desde el punto de vista de quien llama: si no hay comprobante
     * para ese pedido (todavía no se entregó, o falló la emisión original), no es
     * un error — simplemente no hay nada que sincronizar todavía.</p>
     */
    @Override
    @Transactional
    public void actualizarMontoPorPedido(Long pedidoId, java.math.BigDecimal nuevoMonto) {
        repository.findByPedidoId(pedidoId).ifPresent(c -> {
            if (nuevoMonto.compareTo(c.getMontoPagado()) < 0) {
                throw new BusinessException(
                    "El nuevo monto ($" + nuevoMonto + ") es menor a lo ya cobrado ($" + c.getMontoPagado() +
                    "). Si hay que devolver plata, registrá un reintegro en vez de editar el comprobante.");
            }
            c.setMonto(nuevoMonto);
            // Si el ajuste hace que lo ya pagado cubra el nuevo monto entero, se salda solo.
            if (c.getMontoPagado().compareTo(nuevoMonto) >= 0 && c.getEstadoPago() != EstadoPago.COBRADO) {
                c.setEstadoPago(EstadoPago.COBRADO);
                c.setFechaCobro(LocalDate.now());
            } else if (c.getMontoPagado().signum() > 0 && c.getEstadoPago() == EstadoPago.PENDIENTE) {
                c.setEstadoPago(EstadoPago.PARCIAL);
            }
            repository.save(c);
        });
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

        auditoria.registrar("PAGO", "Pago a cuenta corriente", "Odontólogo " + nombre,
                "$" + imputado + " por " + req.medio() + " · " + afectados + " comprobante(s)");

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
        return mapearCuentas(repository.rankingDeudoresRaw());
    }

    @Override
    public List<CuentaCorrienteOdontologoResponse> listarTodasCuentas() {
        return mapearCuentas(repository.rankingTodosRaw());
    }

    /** Mapeo común de la proyección de cuenta corriente — compartido entre "solo morosos" y "todos". */
    private List<CuentaCorrienteOdontologoResponse> mapearCuentas(List<Object[]> rows) {
        LocalDate hoy = LocalDate.now();

        return rows.stream()
                .map(r -> {
                    Long odontologoId        = (Long) r[0];
                    String odontologoNombre  = (String) r[1];
                    BigDecimal totalDeuda    = (BigDecimal) r[2];
                    long comprobantes        = (Long) r[3];
                    LocalDate fechaMasVieja  = (LocalDate) r[4];

                    // Si ya no tiene deuda (pagó todo), "días sin pagar" no aplica —
                    // sin este chequeo quedaba calculado contra el comprobante más
                    // viejo de su historial entero, aunque esté saldado hace rato.
                    long diasSinPagar = (totalDeuda.signum() > 0 && fechaMasVieja != null)
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

    /**
     * Numera el comprobante siguiendo al último emitido ESTE MES, no contando
     * filas de toda la tabla.
     *
     * <p>Antes usaba {@code repository.count() + 1}, que se rompe apenas se borra
     * un comprobante: el contador retrocede y vuelve a generar un número que ya
     * existe. Como {@code nro_comprobante} es UNIQUE, esa inserción falla, y el
     * error se lo tragaba el best-effort de ms-pedidos → el pedido quedaba
     * ENTREGADO pero SIN deuda emitida, y el saldo del odontólogo nunca
     * cambiaba.</p>
     */
    private String generarNroComprobante() {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String prefijo = String.format("COMP-%s-", fecha);
        String ultimo = repository.maxNroComprobanteConPrefijo(prefijo);

        long siguiente = 1;
        if (ultimo != null && ultimo.length() > prefijo.length()) {
            try {
                siguiente = Long.parseLong(ultimo.substring(prefijo.length())) + 1;
            } catch (NumberFormatException e) {
                // Número con formato inesperado (cargado a mano, migración vieja):
                // se cae al conteo total para no bloquear la emisión.
                siguiente = repository.count() + 1;
            }
        }
        return String.format("%s%04d", prefijo, siguiente);
    }
}
