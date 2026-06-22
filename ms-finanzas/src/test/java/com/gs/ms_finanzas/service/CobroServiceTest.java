package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.CobroRequest;
import com.gs.ms_finanzas.dto.RegistroCobroResponse;
import com.gs.ms_finanzas.exception.BusinessException;
import com.gs.ms_finanzas.exception.ResourceNotFoundException;
import com.gs.ms_finanzas.model.*;
import com.gs.ms_finanzas.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CobroServiceTest {

    @Mock private ComprobanteRepository comprobanteRepo;
    @Mock private CajaMovimientoRepository cajaRepo;
    @Mock private SueldoEmpleadoRepository sueldoRepo;
    @Mock private ProveedorRepository proveedorRepo;
    @Mock private DeudaProveedorRepository deudaRepo;
    @InjectMocks private CobroService service;

    private Comprobante comp(EstadoPago estado) {
        return Comprobante.builder().id(1L).nroComprobante("COMP-1")
                .odontologoId(7L).odontologoNombre("Garcia")
                .monto(new BigDecimal("10000")).estadoPago(estado).build();
    }

    private CobroRequest req(BigDecimal monto, TipoCobro tipo, Long deudaId) {
        return new CobroRequest(1L, monto, tipo, null, deudaId, "obs");
    }

    @Test
    void comprobanteInexistente_404() {
        when(comprobanteRepo.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.registrarCobro(req(new BigDecimal("100"), TipoCobro.EFECTIVO, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void comprobanteYaCobrado_business() {
        when(comprobanteRepo.findById(1L)).thenReturn(Optional.of(comp(EstadoPago.COBRADO)));
        assertThatThrownBy(() -> service.registrarCobro(req(new BigDecimal("100"), TipoCobro.EFECTIVO, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void directo_efectivo_cubreSueldoYRemanenteACaja() {
        when(comprobanteRepo.findById(1L)).thenReturn(Optional.of(comp(EstadoPago.PENDIENTE)));
        SueldoEmpleado s = SueldoEmpleado.builder().empleadoId(1L).empleadoNombre("Ana")
                .monto(new BigDecimal("3000")).mes(1).anio(2026).estado(EstadoSueldo.PENDIENTE).build();
        when(sueldoRepo.findByAnioAndMesAndEstadoOrderByEmpleadoNombreAsc(anyInt(), anyInt(), eq(EstadoSueldo.PENDIENTE)))
                .thenReturn(List.of(s));

        RegistroCobroResponse r = service.registrarCobro(req(new BigDecimal("10000"), TipoCobro.EFECTIVO, null));

        assertThat(s.getEstado()).isEqualTo(EstadoSueldo.PAGADO);
        assertThat(r.cajaDestino()).isEqualTo(TipoCaja.FISICA);
        verify(cajaRepo).saveAll(any());
        verify(comprobanteRepo).save(any());
    }

    @Test
    void directo_transferencia_vaABancaria() {
        when(comprobanteRepo.findById(1L)).thenReturn(Optional.of(comp(EstadoPago.PENDIENTE)));
        when(sueldoRepo.findByAnioAndMesAndEstadoOrderByEmpleadoNombreAsc(anyInt(), anyInt(), any()))
                .thenReturn(List.of());

        RegistroCobroResponse r = service.registrarCobro(req(new BigDecimal("10000"), TipoCobro.TRANSFERENCIA, null));

        assertThat(r.cajaDestino()).isEqualTo(TipoCaja.BANCARIA);
    }

    @Test
    void triangulado_sinDeudaId_business() {
        when(comprobanteRepo.findById(1L)).thenReturn(Optional.of(comp(EstadoPago.PENDIENTE)));
        assertThatThrownBy(() -> service.registrarCobro(req(new BigDecimal("500"), TipoCobro.TRIANGULADO, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void triangulado_deudaYaPagada_business() {
        when(comprobanteRepo.findById(1L)).thenReturn(Optional.of(comp(EstadoPago.PENDIENTE)));
        DeudaProveedor d = DeudaProveedor.builder().id(5L).monto(new BigDecimal("500"))
                .estado(EstadoDeuda.PAGADO).proveedor(Proveedor.builder().nombre("Prov").build()).build();
        when(deudaRepo.findById(5L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.registrarCobro(req(new BigDecimal("500"), TipoCobro.TRIANGULADO, 5L)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void triangulado_montoNoCoincide_business() {
        when(comprobanteRepo.findById(1L)).thenReturn(Optional.of(comp(EstadoPago.PENDIENTE)));
        DeudaProveedor d = DeudaProveedor.builder().id(5L).monto(new BigDecimal("500"))
                .estado(EstadoDeuda.PENDIENTE).proveedor(Proveedor.builder().nombre("Prov").build()).build();
        when(deudaRepo.findById(5L)).thenReturn(Optional.of(d));

        assertThatThrownBy(() -> service.registrarCobro(req(new BigDecimal("999"), TipoCobro.TRIANGULADO, 5L)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void triangulado_ok_saldaAmbasDeudas() {
        when(comprobanteRepo.findById(1L)).thenReturn(Optional.of(comp(EstadoPago.PENDIENTE)));
        DeudaProveedor d = DeudaProveedor.builder().id(5L).monto(new BigDecimal("500")).descripcion("Insumos")
                .estado(EstadoDeuda.PENDIENTE).proveedor(Proveedor.builder().nombre("Prov").build()).build();
        when(deudaRepo.findById(5L)).thenReturn(Optional.of(d));

        RegistroCobroResponse r = service.registrarCobro(req(new BigDecimal("500"), TipoCobro.TRIANGULADO, 5L));

        assertThat(d.getEstado()).isEqualTo(EstadoDeuda.PAGADO);
        assertThat(r.cajaDestino()).isEqualTo(TipoCaja.COMPENSACION);
        verify(deudaRepo).save(d);
    }
}
