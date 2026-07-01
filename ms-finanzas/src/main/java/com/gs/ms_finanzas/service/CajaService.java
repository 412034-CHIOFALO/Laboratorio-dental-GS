package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.CajaMovimientoRequest;
import com.gs.ms_finanzas.dto.CajaMovimientoResponse;
import com.gs.ms_finanzas.dto.ResumenCajasResponse;
import com.gs.ms_finanzas.model.CajaMovimiento;
import com.gs.ms_finanzas.model.TipoCaja;
import com.gs.ms_finanzas.repository.CajaMovimientoRepository;
import com.gs.ms_finanzas.repository.ConfiguracionSueldoRepository;
import com.gs.ms_finanzas.repository.DeudaProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CajaService implements ICajaService {

    private final CajaMovimientoRepository cajaRepo;
    private final DeudaProveedorRepository deudaRepo;
    private final ConfiguracionSueldoRepository configSueldoRepo;

    public ResumenCajasResponse obtenerResumen() {
        BigDecimal saldoFisica = cajaRepo.calcularSaldo(TipoCaja.FISICA);
        BigDecimal saldoBancaria = cajaRepo.calcularSaldo(TipoCaja.BANCARIA);
        BigDecimal saldoComp = cajaRepo.calcularSaldo(TipoCaja.COMPENSACION);
        BigDecimal deudaProveedores = deudaRepo.sumTotalDeudaPendiente();

        // Lo que el lab le debe a los empleados = devengado acumulado del sistema
        // unico de sueldos (ConfiguracionSueldo). Antes salia del sistema viejo
        // (SueldoEmpleado), que estaba desconectado del devengado real.
        BigDecimal sueldosPendientes = configSueldoRepo.totalDevengado();
        if (sueldosPendientes == null) sueldosPendientes = BigDecimal.ZERO;

        List<String> alertas = new ArrayList<>();
        if (saldoFisica.compareTo(BigDecimal.ZERO) < 0)
            alertas.add("Caja fisica en negativo: $" + saldoFisica);
        if (saldoBancaria.compareTo(BigDecimal.ZERO) < 0)
            alertas.add("Caja bancaria en negativo: $" + saldoBancaria);
        if (saldoComp.compareTo(BigDecimal.ZERO) != 0)
            alertas.add("Caja compensacion desbalanceada: $" + saldoComp);
        if (sueldosPendientes.compareTo(BigDecimal.ZERO) > 0)
            alertas.add("Sueldos devengados a pagar: $" + sueldosPendientes);
        if (deudaProveedores.compareTo(BigDecimal.ZERO) > 0)
            alertas.add("Deuda total proveedores: $" + deudaProveedores);

        return new ResumenCajasResponse(saldoFisica, saldoBancaria, saldoComp, deudaProveedores, sueldosPendientes, alertas);
    }

    public List<CajaMovimientoResponse> listarMovimientosByCaja(TipoCaja tipoCaja) {
        return cajaRepo.findByTipoCajaOrderByFechaMovimientoDesc(tipoCaja).stream()
            .map(CajaMovimientoResponse::from)
            .toList();
    }

    public List<CajaMovimientoResponse> listarMovimientosByPeriodo(LocalDate desde, LocalDate hasta) {
        return cajaRepo.findByFechaMovimientoBetweenOrderByFechaMovimientoDesc(desde, hasta).stream()
            .map(CajaMovimientoResponse::from)
            .toList();
    }

    @Transactional
    public CajaMovimientoResponse registrarMovimiento(CajaMovimientoRequest req, String creadoPor) {
        CajaMovimiento mov = CajaMovimiento.builder()
            .tipo(req.tipo())
            .tipoCaja(req.tipoCaja())
            .concepto(req.concepto().trim())
            .monto(req.monto())
            .referencia(req.referencia())
            .fechaMovimiento(req.fechaMovimiento() != null ? req.fechaMovimiento() : LocalDate.now())
            .creadoPor(creadoPor)
            .build();
        return CajaMovimientoResponse.from(cajaRepo.save(mov));
    }
}
