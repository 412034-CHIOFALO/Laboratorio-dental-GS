package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.CajaMovimientoRequest;
import com.gs.ms_finanzas.dto.CajaMovimientoResponse;
import com.gs.ms_finanzas.dto.ResumenCajasResponse;
import com.gs.ms_finanzas.model.*;
import com.gs.ms_finanzas.repository.CajaMovimientoRepository;
import com.gs.ms_finanzas.repository.ConfiguracionSueldoRepository;
import com.gs.ms_finanzas.repository.DeudaProveedorRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CajaServiceTest {

    @Mock private CajaMovimientoRepository cajaRepo;
    @Mock private DeudaProveedorRepository deudaRepo;
    @Mock private ConfiguracionSueldoRepository configSueldoRepo;
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
        when(configSueldoRepo.totalDevengado()).thenReturn(new BigDecimal("300"));

        ResumenCajasResponse r = service.obtenerResumen();

        // física<0, bancaria<0, compensación≠0, sueldos devengados>0, deuda proveedores>0
        assertThat(r.alertas()).hasSize(5);
    }

    @Test
    void resumen_sinAlertas() {
        when(cajaRepo.calcularSaldo(any())).thenReturn(BigDecimal.ZERO);
        when(deudaRepo.sumTotalDeudaPendiente()).thenReturn(BigDecimal.ZERO);
        when(configSueldoRepo.totalDevengado()).thenReturn(BigDecimal.ZERO);

        assertThat(service.obtenerResumen().alertas()).isEmpty();
    }

    @Test
    void resumen_totalDevengadoNull_noRompe() {
        when(cajaRepo.calcularSaldo(any())).thenReturn(BigDecimal.ZERO);
        when(deudaRepo.sumTotalDeudaPendiente()).thenReturn(BigDecimal.ZERO);
        when(configSueldoRepo.totalDevengado()).thenReturn(null);

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
