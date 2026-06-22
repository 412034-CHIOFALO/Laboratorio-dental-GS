package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.CobroRequest;
import com.gs.ms_finanzas.dto.RegistroCobroResponse;
import com.gs.ms_finanzas.exception.BusinessException;
import com.gs.ms_finanzas.exception.ResourceNotFoundException;
import com.gs.ms_finanzas.model.*;
import com.gs.ms_finanzas.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CobroService implements ICobroService {

    private final ComprobanteRepository comprobanteRepo;
    private final CajaMovimientoRepository cajaRepo;
    private final SueldoEmpleadoRepository sueldoRepo;
    private final ProveedorRepository proveedorRepo;
    private final DeudaProveedorRepository deudaRepo;

    private static final Logger log = LoggerFactory.getLogger(CobroService.class);

    @Transactional
    public RegistroCobroResponse registrarCobro(CobroRequest request) {
        // 1. Validar comprobante
        Comprobante comprobante = comprobanteRepo.findById(request.comprobanteId())
            .orElseThrow(() -> new ResourceNotFoundException("Comprobante", request.comprobanteId()));

        if (comprobante.getEstadoPago() == EstadoPago.COBRADO) {
            throw new BusinessException("El comprobante " + comprobante.getNroComprobante() + " ya fue cobrado.");
        }

        // 2. Pago triangulado tiene su propio flujo
        if (request.tipoCobro() == TipoCobro.TRIANGULADO) {
            return registrarPagoTriangulado(request, comprobante);
        }

        // 3. Flujo efectivo/transferencia con cascada de sueldos
        return registrarPagoDirecto(request, comprobante);
    }

    private RegistroCobroResponse registrarPagoDirecto(CobroRequest request, Comprobante comprobante) {
        BigDecimal saldoDisponible = request.monto();
        List<String> sueldosCubiertos = new ArrayList<>();
        List<CajaMovimiento> movimientos = new ArrayList<>();

        // Paso 1: Cubrir sueldos pendientes del mes actual
        int mes = LocalDate.now().getMonthValue();
        int anio = LocalDate.now().getYear();
        List<SueldoEmpleado> pendientes = sueldoRepo
            .findByAnioAndMesAndEstadoOrderByEmpleadoNombreAsc(anio, mes, EstadoSueldo.PENDIENTE);

        BigDecimal montoPagoSueldos = BigDecimal.ZERO;
        for (SueldoEmpleado sueldo : pendientes) {
            if (saldoDisponible.compareTo(BigDecimal.ZERO) <= 0) break;
            if (saldoDisponible.compareTo(sueldo.getMonto()) >= 0) {
                saldoDisponible = saldoDisponible.subtract(sueldo.getMonto());
                montoPagoSueldos = montoPagoSueldos.add(sueldo.getMonto());
                sueldo.setEstado(EstadoSueldo.PAGADO);
                sueldo.setFechaPago(LocalDate.now());
                sueldo.setReferenciaComprobante(comprobante.getNroComprobante());
                sueldoRepo.save(sueldo);
                sueldosCubiertos.add(sueldo.getEmpleadoNombre() + " $" + sueldo.getMonto());
                movimientos.add(CajaMovimiento.builder()
                    .tipo(TipoMovimientoCaja.EGRESO)
                    .tipoCaja(TipoCaja.FISICA)
                    .concepto("Sueldo " + sueldo.getEmpleadoNombre() + " " + mes + "/" + anio)
                    .monto(sueldo.getMonto())
                    .referencia(comprobante.getNroComprobante())
                    .build());
                log.info("[GS-FINANZAS] Sueldo cubierto: {} ${}", sueldo.getEmpleadoNombre(), sueldo.getMonto());
            }
        }

        // Paso 2: Remanente va a la caja correspondiente
        TipoCaja cajaDestino = request.tipoCobro() == TipoCobro.TRANSFERENCIA ? TipoCaja.BANCARIA : TipoCaja.FISICA;
        if (saldoDisponible.compareTo(BigDecimal.ZERO) > 0) {
            movimientos.add(CajaMovimiento.builder()
                .tipo(TipoMovimientoCaja.INGRESO)
                .tipoCaja(cajaDestino)
                .concepto("Cobro " + comprobante.getNroComprobante() + " — " + comprobante.getOdontologoNombre())
                .monto(saldoDisponible)
                .referencia(comprobante.getNroComprobante())
                .build());
        }

        // Paso 3: Marcar comprobante cobrado y guardar movimientos
        comprobante.setEstadoPago(EstadoPago.COBRADO);
        comprobante.setFechaCobro(LocalDate.now());
        comprobanteRepo.save(comprobante);
        cajaRepo.saveAll(movimientos);

        String msg = sueldosCubiertos.isEmpty()
            ? "Cobro registrado. $" + request.monto() + " ingresados en caja " + cajaDestino.name().toLowerCase()
            : "Cobro registrado. Sueldos cubiertos: " + sueldosCubiertos.size()
                + ". Restante en caja " + cajaDestino.name().toLowerCase() + ": $" + saldoDisponible;

        return new RegistroCobroResponse(
            comprobante.getNroComprobante(),
            request.monto(),
            montoPagoSueldos,
            saldoDisponible,
            cajaDestino,
            sueldosCubiertos,
            msg
        );
    }

    /**
     * Pago triangulado: el odontologo paga directamente al proveedor.
     * La Caja Compensacion registra el paso del dinero (saldo debe quedar = 0).
     * INGRESO Compensacion = deuda odontologo | EGRESO Compensacion = deuda proveedor
     */
    private RegistroCobroResponse registrarPagoTriangulado(CobroRequest request, Comprobante comprobante) {
        if (request.deudaProveedorId() == null) {
            throw new BusinessException("Para pago triangulado se requiere deudaProveedorId.");
        }
        DeudaProveedor deuda = deudaRepo.findById(request.deudaProveedorId())
            .orElseThrow(() -> new ResourceNotFoundException("DeudaProveedor", request.deudaProveedorId()));

        if (deuda.getEstado() == EstadoDeuda.PAGADO) {
            throw new BusinessException("La deuda con proveedor ya fue saldada.");
        }
        if (request.monto().compareTo(deuda.getMonto()) != 0) {
            throw new BusinessException("El monto del pago ($" + request.monto()
                + ") debe coincidir exactamente con la deuda ($" + deuda.getMonto() + ").");
        }

        // Registrar flujo en Caja Compensacion (debe ser 0 neto)
        List<CajaMovimiento> movs = List.of(
            CajaMovimiento.builder()
                .tipo(TipoMovimientoCaja.INGRESO)
                .tipoCaja(TipoCaja.COMPENSACION)
                .concepto("Triangulado: " + comprobante.getOdontologoNombre()
                    + " paga a " + deuda.getProveedor().getNombre())
                .monto(request.monto())
                .referencia(comprobante.getNroComprobante())
                .build(),
            CajaMovimiento.builder()
                .tipo(TipoMovimientoCaja.EGRESO)
                .tipoCaja(TipoCaja.COMPENSACION)
                .concepto("Triangulado: deuda " + deuda.getProveedor().getNombre()
                    + " — " + deuda.getDescripcion())
                .monto(request.monto())
                .referencia(comprobante.getNroComprobante())
                .build()
        );

        // Marcar ambas deudas como saldadas
        comprobante.setEstadoPago(EstadoPago.COBRADO);
        comprobante.setFechaCobro(LocalDate.now());
        deuda.setEstado(EstadoDeuda.PAGADO);
        deuda.setFechaPago(LocalDate.now());

        comprobanteRepo.save(comprobante);
        deudaRepo.save(deuda);
        cajaRepo.saveAll(movs);

        return new RegistroCobroResponse(
            comprobante.getNroComprobante(),
            request.monto(),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            TipoCaja.COMPENSACION,
            List.of(),
            "Pago triangulado registrado. Caja compensacion: $0 neto."
        );
    }
}
