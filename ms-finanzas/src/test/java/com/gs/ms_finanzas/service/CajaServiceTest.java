package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.CajaMovimientoRequest;
import com.gs.ms_finanzas.dto.CajaMovimientoResponse;
import com.gs.ms_finanzas.dto.ResumenCajasResponse;
import com.gs.ms_finanzas.model.*;
import com.gs.ms_finanzas.repository.CajaMovimientoRepository;
import com.gs.ms_finanzas.repository.DeudaProveedorRepository;
import com.gs.ms_finanzas.repository.SueldoEmpleadoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CajaServiceTest {

    @Mock private CajaMovimientoRepository cajaRepo;
    @Mock private DeudaProveedorRepository deudaRepo;
    @Mock private SueldoEmpleadoRepository sueldoRepo;
    @InjectMocks private CajaService service;

    private CajaMovimiento mov() {
        return CajaMovimiento.builder()
                .tipo(TipoMovimientoCaja.INGRESO).tipoCaja(TipoCaja.FISICA)
                .concepto("Cobro").monto(new BigDecimal("100"))
                .fechaMovimiento(LocalDate.now()).creadoPor("admin").build();
    }

    @Test
    void resumen_conTodasLasAlertas() {
        when(cajaRepo.calcularSaldo(TipoCaja.FISICA)).thenReturn(new BigDecimal("-50"));
        when(cajaRepo.calcularSaldo(TipoCaja.BANCARIA)).thenReturn(new BigDecimal("-10"));
        when(cajaRepo.calcularSaldo(TipoCaja.COMPENSACION)).thenReturn(new BigDecimal("5"));
        when(deudaRepo.sumTotalDeudaPendiente()).thenReturn(new BigDecimal("200"));
        SueldoEmpleado s = SueldoEmpleado.builder().empleadoId(1L).empleadoNombre("Ana")
                .monto(new BigDecimal("300")).mes(1).anio(2026).estado(EstadoSueldo.PENDIENTE).build();
        when(sueldoRepo.findByAnioAndMesAndEstadoOrderByEmpleadoNombreAsc(anyIntEq(), anyIntEq(), eq(EstadoSueldo.PENDIENTE)))
                .thenReturn(List.of(s));

        ResumenCajasResponse r = service.obtenerResumen();

        assertThat(r.alertas()).hasSize(5);
    }

    // helper para que Mockito acepte cualquier int (anio/mes calculados de LocalDate.now)
    private int anyIntEq() { return org.mockito.ArgumentMatchers.anyInt(); }

    @Test
    void resumen_sinAlertas() {
        when(cajaRepo.calcularSaldo(any())).thenReturn(BigDecimal.ZERO);
        when(deudaRepo.sumTotalDeudaPendiente()).thenReturn(BigDecimal.ZERO);
        when(sueldoRepo.findByAnioAndMesAndEstadoOrderByEmpleadoNombreAsc(anyIntEq(), anyIntEq(), any()))
                .thenReturn(List.of());

        assertThat(service.obtenerResumen().alertas()).isEmpty();
    }

    @Test
    void listarPorCaja_mapea() {
        when(cajaRepo.findByTipoCajaOrderByFechaMovimientoDesc(TipoCaja.FISICA)).thenReturn(List.of(mov()));
        List<CajaMovimientoResponse> r = service.listarMovimientosByCaja(TipoCaja.FISICA);
        assertThat(r).hasSize(1);
    }

    @Test
    void listarPorPeriodo_mapea() {
        when(cajaRepo.findByFechaMovimientoBetweenOrderByFechaMovimientoDesc(any(), any())).thenReturn(List.of(mov()));
        assertThat(service.listarMovimientosByPeriodo(LocalDate.now().minusDays(7), LocalDate.now())).hasSize(1);
    }

    @Test
    void registrarMovimiento_guarda() {
        when(cajaRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        CajaMovimientoRequest req = new CajaMovimientoRequest(
                TipoMovimientoCaja.EGRESO, TipoCaja.BANCARIA, "  Pago  ",
                new BigDecimal("80"), "ref-1", null, null);

        CajaMovimientoResponse r = service.registrarMovimiento(req, "admin");

        assertThat(r).isNotNull();
    }
}
