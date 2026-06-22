package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.ComprobanteRequest;
import com.gs.ms_finanzas.dto.ComprobanteResponse;
import com.gs.ms_finanzas.dto.CuentaCorrienteOdontologoResponse;
import com.gs.ms_finanzas.exception.BusinessException;
import com.gs.ms_finanzas.exception.ResourceNotFoundException;
import com.gs.ms_finanzas.model.Comprobante;
import com.gs.ms_finanzas.model.EstadoPago;
import com.gs.ms_finanzas.repository.ComprobanteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanzasServiceTest {

    @Mock private ComprobanteRepository repository;
    @InjectMocks private FinanzasService service;

    private Comprobante comp(EstadoPago estado) {
        return Comprobante.builder()
                .id(1L).nroComprobante("COMP-202601-0001")
                .odontologoId(7L).odontologoNombre("Garcia")
                .monto(new BigDecimal("10000")).estadoPago(estado)
                .fechaEmision(LocalDate.now()).build();
    }

    @Test
    void listarTodos_mapea() {
        when(repository.findAll()).thenReturn(List.of(comp(EstadoPago.PENDIENTE)));
        assertThat(service.listarTodos()).hasSize(1);
    }

    @Test
    void listarPorOdontologo_mapea() {
        when(repository.findByOdontologoId(7L)).thenReturn(List.of(comp(EstadoPago.PENDIENTE)));
        assertThat(service.listarPorOdontologo(7L)).hasSize(1);
    }

    @Test
    void listarPendientes_mapea() {
        when(repository.findByEstadoPago(EstadoPago.PENDIENTE)).thenReturn(List.of(comp(EstadoPago.PENDIENTE)));
        assertThat(service.listarPendientes()).hasSize(1);
    }

    @Test
    void saldoPendiente_delega() {
        when(repository.sumMontosPendientesByOdontologo(7L)).thenReturn(new BigDecimal("5000"));
        assertThat(service.saldoPendienteOdontologo(7L)).isEqualByComparingTo("5000");
    }

    @Test
    void buscarPorId_404() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscarPorId(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void emitir_generaNroYGuarda() {
        when(repository.count()).thenReturn(0L);
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        ComprobanteRequest r = new ComprobanteRequest();
        r.setOdontologoId(7L); r.setOdontologoNombre("Garcia");
        r.setMonto(new BigDecimal("10000")); r.setTrabajo("Corona");
        assertThat(service.emitir(r)).isNotNull();
    }

    @Test
    void registrarCobro_yaCobrado_business() {
        when(repository.findById(1L)).thenReturn(Optional.of(comp(EstadoPago.COBRADO)));
        assertThatThrownBy(() -> service.registrarCobro(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void registrarCobro_ok_marcaCobrado() {
        Comprobante c = comp(EstadoPago.PENDIENTE);
        when(repository.findById(1L)).thenReturn(Optional.of(c));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        service.registrarCobro(1L);
        assertThat(c.getEstadoPago()).isEqualTo(EstadoPago.COBRADO);
    }

    @Test
    void rankingMorosos_clasificaSeveridad() {
        Object[] critica = {1L, "Critico", new BigDecimal("600000"), 3L, LocalDate.now().minusDays(120)};
        Object[] media   = {2L, "Medio", new BigDecimal("60000"), 1L, LocalDate.now().minusDays(40)};
        Object[] alDia   = {3L, "AlDia", BigDecimal.ZERO, 0L, null};
        when(repository.rankingDeudoresRaw()).thenReturn(List.of(critica, media, alDia));

        List<CuentaCorrienteOdontologoResponse> r = service.rankingMorosos();

        assertThat(r).hasSize(3);
        assertThat(r.get(0).severidad()).isEqualTo(CuentaCorrienteOdontologoResponse.Severidad.CRITICA);
        assertThat(r.get(2).severidad()).isEqualTo(CuentaCorrienteOdontologoResponse.Severidad.AL_DIA);
    }
}
