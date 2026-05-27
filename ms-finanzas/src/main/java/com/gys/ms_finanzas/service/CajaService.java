package com.gys.ms_finanzas.service;

import com.gys.ms_finanzas.dto.CajaMovimientoResponse;
import com.gys.ms_finanzas.dto.ResumenCajasResponse;
import com.gys.ms_finanzas.model.EstadoSueldo;
import com.gys.ms_finanzas.model.SueldoEmpleado;
import com.gys.ms_finanzas.model.TipoCaja;
import com.gys.ms_finanzas.repository.CajaMovimientoRepository;
import com.gys.ms_finanzas.repository.DeudaProveedorRepository;
import com.gys.ms_finanzas.repository.SueldoEmpleadoRepository;
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
    private final SueldoEmpleadoRepository sueldoRepo;

    public ResumenCajasResponse obtenerResumen() {
        BigDecimal saldoFisica = cajaRepo.calcularSaldo(TipoCaja.FISICA);
        BigDecimal saldoBancaria = cajaRepo.calcularSaldo(TipoCaja.BANCARIA);
        BigDecimal saldoComp = cajaRepo.calcularSaldo(TipoCaja.COMPENSACION);
        BigDecimal deudaProveedores = deudaRepo.sumTotalDeudaPendiente();

        int mes = LocalDate.now().getMonthValue();
        int anio = LocalDate.now().getYear();
        BigDecimal sueldosPendientes = sueldoRepo
            .findByAnioAndMesAndEstadoOrderByEmpleadoNombreAsc(anio, mes, EstadoSueldo.PENDIENTE)
            .stream()
            .map(SueldoEmpleado::getMonto)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> alertas = new ArrayList<>();
        if (saldoFisica.compareTo(BigDecimal.ZERO) < 0)
            alertas.add("Caja fisica en negativo: $" + saldoFisica);
        if (saldoBancaria.compareTo(BigDecimal.ZERO) < 0)
            alertas.add("Caja bancaria en negativo: $" + saldoBancaria);
        if (saldoComp.compareTo(BigDecimal.ZERO) != 0)
            alertas.add("Caja compensacion desbalanceada: $" + saldoComp);
        if (sueldosPendientes.compareTo(BigDecimal.ZERO) > 0)
            alertas.add("Sueldos pendientes del mes: $" + sueldosPendientes);
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
}
