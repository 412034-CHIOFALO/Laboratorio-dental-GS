package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.ComprobanteRequest;
import com.gs.ms_finanzas.dto.ComprobanteResponse;
import com.gs.ms_finanzas.dto.CuentaCorrienteOdontologoResponse;
import com.gs.ms_finanzas.dto.PagoCuentaCorrienteRequest;
import com.gs.ms_finanzas.dto.PagoCuentaCorrienteResponse;
import com.gs.ms_finanzas.exception.BusinessException;
import com.gs.ms_finanzas.exception.ResourceNotFoundException;
import com.gs.ms_finanzas.model.Comprobante;
import com.gs.ms_finanzas.model.EstadoPago;
import com.gs.ms_finanzas.model.MedioPago;
import com.gs.ms_finanzas.repository.CajaMovimientoRepository;
import com.gs.ms_finanzas.repository.ComprobanteRepository;
import com.gs.ms_finanzas.repository.PagoCuentaCorrienteRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanzasServiceTest {

    @Mock private ComprobanteRepository repository;
    @Mock private CajaMovimientoRepository cajaRepo;
    @Mock private PagoCuentaCorrienteRepository pagoRepo;
    @InjectMocks private FinanzasService service;

    private Comprobante comp(EstadoPago estado) {
        return Comprobante.builder()
                .id(1L).nroComprobante("COMP-202601-0001")
                .odontologoId(7L).odontologoNombre("Garcia")
                .monto(new BigDecimal("10000")).estadoPago(estado)
                .fechaEmision(LocalDate.now()).build();
    }

    private Comprobante comp(long id, String monto, int diasAtras) {
        return Comprobante.builder()
                .id(id).nroComprobante("COMP-" + id)
                .odontologoId(7L).odontologoNombre("Garcia")
                .monto(new BigDecimal(monto)).montoPagado(BigDecimal.ZERO)
                .estadoPago(EstadoPago.PENDIENTE)
                .fechaEmision(LocalDate.now().minusDays(diasAtras)).build();
    }

    private PagoCuentaCorrienteRequest pago(String monto, MedioPago medio) {
        return new PagoCuentaCorrienteRequest(new BigDecimal(monto), medio, null, null);
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
        when(repository.findByPedidoId(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        ComprobanteRequest r = new ComprobanteRequest();
        r.setPedidoId(1L);
        r.setOdontologoId(7L); r.setOdontologoNombre("Garcia");
        r.setMonto(new BigDecimal("10000")); r.setTrabajo("Corona");
        assertThat(service.emitir(r)).isNotNull();
    }

    /**
     * La numeración sigue al último comprobante DEL MES, no a count(): si se
     * borró alguno, count() retrocede y regenera un número ya usado (UNIQUE),
     * la inserción falla y el pedido queda entregado sin deuda emitida.
     */
    @Test
    void emitir_numeraSiguiendoAlUltimoDelMes_noAlConteo() {
        when(repository.findByPedidoId(any())).thenReturn(Optional.empty());
        when(repository.maxNroComprobanteConPrefijo(any())).thenReturn("COMP-202607-0009");
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        ComprobanteRequest r = new ComprobanteRequest();
        r.setPedidoId(1L);
        r.setOdontologoId(7L); r.setOdontologoNombre("Garcia");
        r.setMonto(new BigDecimal("10000")); r.setTrabajo("Corona");

        assertThat(service.emitir(r).nroComprobante()).endsWith("-0010");
    }

    /** Reemitir el mismo pedido devuelve el comprobante existente, no duplica la deuda. */
    @Test
    void emitir_pedidoYaFacturado_devuelveElExistenteSinDuplicar() {
        Comprobante existente = comp(EstadoPago.PENDIENTE);
        when(repository.findByPedidoId(42L)).thenReturn(Optional.of(existente));

        ComprobanteRequest r = new ComprobanteRequest();
        r.setPedidoId(42L);
        r.setOdontologoId(7L); r.setOdontologoNombre("Garcia");
        r.setMonto(new BigDecimal("10000")); r.setTrabajo("Corona");

        assertThat(service.emitir(r).nroComprobante()).isEqualTo(existente.getNroComprobante());
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).save(any());
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

    // ── Pago a cuenta corriente ──────────────────────────────────────

    @Test
    void pagoCC_sinDeuda_business() {
        when(repository.findByOdontologoIdAndEstadoPagoIn(eq(7L), any())).thenReturn(List.of());
        assertThatThrownBy(() -> service.registrarPagoCuentaCorriente(7L, pago("1000", MedioPago.EFECTIVO)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void pagoCC_total_marcaCobrado_eIngresaCaja() {
        Comprobante c = comp(1L, "10000", 5);
        when(repository.findByOdontologoIdAndEstadoPagoIn(eq(7L), any())).thenReturn(List.of(c));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pagoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repository.sumMontosPendientesByOdontologo(7L)).thenReturn(BigDecimal.ZERO);

        PagoCuentaCorrienteResponse res = service.registrarPagoCuentaCorriente(7L, pago("10000", MedioPago.TRANSFERENCIA));

        assertThat(c.getEstadoPago()).isEqualTo(EstadoPago.COBRADO);
        assertThat(c.getMontoPagado()).isEqualByComparingTo("10000");
        assertThat(res.montoImputado()).isEqualByComparingTo("10000");
        assertThat(res.comprobantesAfectados()).isEqualTo(1);
        org.mockito.Mockito.verify(cajaRepo).save(any());  // ingresó a caja
    }

    @Test
    void pagoCC_parcial_marcaParcial() {
        Comprobante c = comp(1L, "10000", 5);
        when(repository.findByOdontologoIdAndEstadoPagoIn(eq(7L), any())).thenReturn(List.of(c));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pagoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repository.sumMontosPendientesByOdontologo(7L)).thenReturn(new BigDecimal("4000"));

        service.registrarPagoCuentaCorriente(7L, pago("6000", MedioPago.EFECTIVO));

        assertThat(c.getEstadoPago()).isEqualTo(EstadoPago.PARCIAL);
        assertThat(c.getMontoPagado()).isEqualByComparingTo("6000");
    }

    @Test
    void pagoCC_imputaViejoPrimero_yExcedenteAvisa() {
        Comprobante viejo = comp(1L, "5000", 30);
        Comprobante nuevo = comp(2L, "5000", 2);
        // El servicio ordena por fechaEmision asc; devolvemos desordenado a propósito.
        when(repository.findByOdontologoIdAndEstadoPagoIn(eq(7L), any())).thenReturn(List.of(nuevo, viejo));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pagoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(repository.sumMontosPendientesByOdontologo(7L)).thenReturn(BigDecimal.ZERO);

        PagoCuentaCorrienteResponse res = service.registrarPagoCuentaCorriente(7L, pago("12000", MedioPago.EFECTIVO));

        assertThat(viejo.getEstadoPago()).isEqualTo(EstadoPago.COBRADO);
        assertThat(nuevo.getEstadoPago()).isEqualTo(EstadoPago.COBRADO);
        assertThat(res.montoImputado()).isEqualByComparingTo("10000");   // solo lo que había de deuda
        assertThat(res.comprobantesAfectados()).isEqualTo(2);
        assertThat(res.mensaje()).contains("Excedente");                 // $2000 sobrante
    }
}
