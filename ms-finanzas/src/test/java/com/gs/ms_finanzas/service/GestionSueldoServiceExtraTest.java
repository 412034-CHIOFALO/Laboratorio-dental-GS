package com.gs.ms_finanzas.service;

import com.gs.ms_finanzas.dto.*;
import com.gs.ms_finanzas.exception.BusinessException;
import com.gs.ms_finanzas.exception.ResourceNotFoundException;
import com.gs.ms_finanzas.model.*;
import com.gs.ms_finanzas.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Cobertura extendida de GestionSueldoService: pagos manuales, devengado,
 * efectivo (borrador/confirmar/rechazar), triangulado, urls de comprobante.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestionSueldoServiceExtraTest {

    @Mock private ConfiguracionSueldoRepository configRepo;
    @Mock private PagoSueldoRepository pagoRepo;
    @Mock private MinioStorageService minioStorage;
    @Mock private ProveedorRepository proveedorRepo;
    @Mock private RegistroPagoBotRepository registroRepo;
    @Mock private ComprobanteRepository comprobanteRepo;
    @Mock private DeudaProveedorRepository deudaProveedorRepo;
    @Mock private CajaMovimientoRepository cajaMovimientoRepo;
    @InjectMocks private GestionSueldoService service;

    private ConfiguracionSueldo config(boolean activo, String devengado) {
        return ConfiguracionSueldo.builder()
                .empleadoId(2L).empleadoNombre("Carlos Lopez").activo(activo)
                .saldoDevengado(new BigDecimal(devengado)).saldoSobrante(BigDecimal.ZERO)
                .frecuencia(FrecuenciaPago.MENSUAL).build();
    }

    private void stubSave() {
        when(registroRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pagoRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(configRepo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ── Consultas ───────────────────────────────────────────────
    @Test
    void listarEmpleados_mapea() {
        when(configRepo.findAllByOrderByEmpleadoNombreAsc()).thenReturn(List.of(config(true, "50000")));
        assertThat(service.listarEmpleados()).hasSize(1);
    }

    @Test
    void buscarEmpleado_inexistente_404() {
        when(configRepo.findByEmpleadoId(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscarEmpleado(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void guardarConfig_ok() {
        when(configRepo.findByEmpleadoId(2L)).thenReturn(Optional.of(config(true, "50000")));
        when(configRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        ConfigSueldoRequest r = new ConfigSueldoRequest();
        r.setFrecuencia(FrecuenciaPago.MENSUAL);
        r.setMontoBase(new BigDecimal("80000"));
        assertThat(service.guardarConfig(2L, r)).isNotNull();
    }

    // ── Pago manual / aplicarPago ───────────────────────────────
    @Test
    void registrarPago_descuentaDelDevengado() {
        when(configRepo.findByEmpleadoId(2L)).thenReturn(Optional.of(config(true, "50000")));
        stubSave();
        PagoSueldoRequest r = new PagoSueldoRequest();
        r.setUsuarioId(2L); r.setMonto(new BigDecimal("30000"));
        assertThat(service.registrarPago(r)).isNotNull();
    }

    @Test
    void registrarPago_excedente_vaASobrante() {
        ConfiguracionSueldo c = config(true, "10000");
        when(configRepo.findByEmpleadoId(2L)).thenReturn(Optional.of(c));
        stubSave();
        PagoSueldoRequest r = new PagoSueldoRequest();
        r.setUsuarioId(2L); r.setMonto(new BigDecimal("15000")); // excede 10000
        service.registrarPago(r);
        assertThat(c.getSaldoSobrante()).isEqualByComparingTo("5000");
        assertThat(c.getSaldoDevengado()).isEqualByComparingTo("0");
    }

    @Test
    void registrarPago_montoCero_business() {
        when(configRepo.findByEmpleadoId(2L)).thenReturn(Optional.of(config(true, "50000")));
        PagoSueldoRequest r = new PagoSueldoRequest();
        r.setUsuarioId(2L); r.setMonto(BigDecimal.ZERO);
        assertThatThrownBy(() -> service.registrarPago(r)).isInstanceOf(BusinessException.class);
    }

    @Test
    void registrarPago_empleadoInactivo_business() {
        when(configRepo.findByEmpleadoId(2L)).thenReturn(Optional.of(config(false, "50000")));
        PagoSueldoRequest r = new PagoSueldoRequest();
        r.setUsuarioId(2L); r.setMonto(new BigDecimal("100"));
        assertThatThrownBy(() -> service.registrarPago(r)).isInstanceOf(BusinessException.class);
    }

    // ── Bot automatico: duplicado / inactivo / triangulado ──────
    @Test
    void automatico_duplicado() {
        when(registroRepo.existsByIdOperacionAndEstado("OP-DUP", EstadoRegistroBot.REGISTRADO)).thenReturn(true);
        when(registroRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        PagoAutomaticoRequest r = new PagoAutomaticoRequest();
        r.setReceptorNombre("Carlos"); r.setIdOperacion("OP-DUP"); r.setMonto(new BigDecimal("100"));

        RegistroPagoBotResponse res = service.registrarPagoAutomatico(r);
        assertThat(res.estado()).isEqualTo(EstadoRegistroBot.DUPLICADO);
    }

    @Test
    void automatico_empleadoInactivo_rechazado() {
        when(configRepo.findAllByOrderByEmpleadoNombreAsc()).thenReturn(List.of(config(false, "50000")));
        when(registroRepo.existsByIdOperacionAndEstado(anyString(), any())).thenReturn(false);
        when(registroRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        PagoAutomaticoRequest r = new PagoAutomaticoRequest();
        r.setReceptorNombre("Carlos"); r.setIdOperacion("OP-X"); r.setMonto(new BigDecimal("100"));

        RegistroPagoBotResponse res = service.registrarPagoAutomatico(r);
        assertThat(res.estado()).isEqualTo(EstadoRegistroBot.RECHAZADO);
        assertThat(res.tipoReceptor()).isEqualTo(TipoReceptorBot.EMPLEADO);
    }

    @Test
    void automatico_triangulado_proveedorConOdontologoEmisor() {
        Proveedor p = Proveedor.builder().id(3L).nombre("Dental SA").activo(true).build();
        Comprobante oc = Comprobante.builder().id(1L).odontologoId(7L).odontologoNombre("Martin Garcia")
                .monto(new BigDecimal("5000")).estadoPago(EstadoPago.PENDIENTE).fechaEmision(LocalDate.now()).build();
        when(configRepo.findAllByOrderByEmpleadoNombreAsc()).thenReturn(List.of());
        when(proveedorRepo.findByActivoTrue()).thenReturn(List.of(p));
        when(comprobanteRepo.findAll()).thenReturn(List.of(oc));
        when(comprobanteRepo.findByOdontologoIdAndEstadoPago(7L, EstadoPago.PENDIENTE)).thenReturn(List.of(oc));
        when(deudaProveedorRepo.findByProveedorIdOrderByFechaCreacionDesc(3L)).thenReturn(List.of());
        when(registroRepo.existsByIdOperacionAndEstado(anyString(), any())).thenReturn(false);
        when(registroRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        PagoAutomaticoRequest r = new PagoAutomaticoRequest();
        r.setReceptorNombre("Dental"); r.setEmisor("Garcia"); r.setIdOperacion("OP-TRI");
        r.setMonto(new BigDecimal("5000"));

        RegistroPagoBotResponse res = service.registrarPagoAutomatico(r);
        assertThat(res.tipoReceptor()).isEqualTo(TipoReceptorBot.PROVEEDOR);
        assertThat(res.mensaje()).contains("Triangulado");
    }

    /**
     * Caso del profe: una misma entidad es proveedor del lab Y odontólogo cliente.
     * No debe "triangular contra sí misma" → se registra como pago directo al
     * proveedor (no triangulado).
     */
    @Test
    void automatico_proveedorEsTambienOdontologo_noTriangulaContraSiMismo() {
        Proveedor p = Proveedor.builder().id(9L).nombre("Garcia Dental").activo(true).build();
        Comprobante oc = Comprobante.builder().id(2L).odontologoId(9L).odontologoNombre("Garcia Dental")
                .monto(new BigDecimal("5000")).estadoPago(EstadoPago.PENDIENTE).fechaEmision(LocalDate.now()).build();
        when(configRepo.findAllByOrderByEmpleadoNombreAsc()).thenReturn(List.of());
        when(proveedorRepo.findByActivoTrue()).thenReturn(List.of(p));
        when(comprobanteRepo.findAll()).thenReturn(List.of(oc));
        when(deudaProveedorRepo.findByProveedorIdOrderByFechaCreacionDesc(9L)).thenReturn(List.of());
        when(registroRepo.existsByIdOperacionAndEstado(anyString(), any())).thenReturn(false);
        when(registroRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        PagoAutomaticoRequest r = new PagoAutomaticoRequest();
        r.setReceptorNombre("Garcia Dental"); r.setEmisor("Garcia Dental"); r.setIdOperacion("OP-SELF");
        r.setMonto(new BigDecimal("5000"));

        RegistroPagoBotResponse res = service.registrarPagoAutomatico(r);
        assertThat(res.tipoReceptor()).isEqualTo(TipoReceptorBot.PROVEEDOR);
        assertThat(res.mensaje()).doesNotContain("Triangulado");
        assertThat(res.mensaje()).contains("Pago a proveedor");
    }

    // ── Devengado ───────────────────────────────────────────────
    @Test
    void ajustarDevengado_negativo_business() {
        assertThatThrownBy(() -> service.ajustarDevengado(2L, new BigDecimal("-1")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void ajustarDevengado_ok() {
        when(configRepo.findByEmpleadoId(2L)).thenReturn(Optional.of(config(true, "0")));
        when(configRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.ajustarDevengado(2L, new BigDecimal("99999"))).isNotNull();
    }

    // ── Listados varios ─────────────────────────────────────────
    @Test
    void listados_ok() {
        when(pagoRepo.findByEmpleadoIdOrderByFechaDescIdDesc(2L)).thenReturn(List.of());
        when(pagoRepo.findAllByOrderByFechaDescIdDesc()).thenReturn(List.of());
        when(registroRepo.findAllByOrderByFechaHoraDesc()).thenReturn(List.of());
        when(configRepo.totalDevengado()).thenReturn(new BigDecimal("1000"));
        when(registroRepo.findByEstadoOrderByFechaHoraDesc(EstadoRegistroBot.PENDIENTE)).thenReturn(List.of());

        assertThat(service.historialPagos(2L)).isEmpty();
        assertThat(service.historialPagosGlobal()).isEmpty();
        assertThat(service.listarRegistrosBot()).isEmpty();
        assertThat(service.totalDevengado()).isEqualByComparingTo("1000");
        assertThat(service.listarPendientesEfectivo()).isEmpty();
    }

    // ── Clave de objeto (MinIO) del comprobante ──────────────────
    @Test
    void objectKeyComprobante_404() {
        when(pagoRepo.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.objectKeyComprobante(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void objectKeyComprobante_sinComprobante_business() {
        when(pagoRepo.findById(1L)).thenReturn(Optional.of(PagoSueldo.builder().id(1L).comprobanteUrl(null).build()));
        assertThatThrownBy(() -> service.objectKeyComprobante(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void objectKeyComprobante_ok() {
        when(pagoRepo.findById(1L)).thenReturn(Optional.of(PagoSueldo.builder().id(1L).comprobanteUrl("obj-1").build()));
        assertThat(service.objectKeyComprobante(1L)).isEqualTo("obj-1");
    }

    @Test
    void objectKeyComprobanteRegistro_ok() {
        when(registroRepo.findById(1L)).thenReturn(Optional.of(
                RegistroPagoBot.builder().id(1L).comprobanteUrl("obj-2").build()));
        assertThat(service.objectKeyComprobanteRegistro(1L)).isEqualTo("obj-2");
    }

    // ── Efectivo ────────────────────────────────────────────────
    private RegistroPagoBot efectivoPendiente(String receptor, String monto) {
        return RegistroPagoBot.builder().id(1L).monto(new BigDecimal(monto))
                .receptorNombre(receptor).estado(EstadoRegistroBot.PENDIENTE)
                .fuente(FuentePago.EFECTIVO).tipoReceptor(TipoReceptorBot.DESCONOCIDO).build();
    }

    @Test
    void registrarPagoEfectivo_creaBorradorPendiente() {
        when(registroRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        PagoEfectivoRequest r = new PagoEfectivoRequest();
        r.setReceptorNombre("Carlos"); r.setMonto(new BigDecimal("3000"));
        RegistroPagoBotResponse res = service.registrarPagoEfectivo(r);
        assertThat(res.estado()).isEqualTo(EstadoRegistroBot.PENDIENTE);
    }

    @Test
    void confirmarEfectivo_empleado() {
        when(registroRepo.findById(1L)).thenReturn(Optional.of(efectivoPendiente("Carlos", "3000")));
        when(configRepo.findAllByOrderByEmpleadoNombreAsc()).thenReturn(List.of(config(true, "50000")));
        stubSave();
        RegistroPagoBotResponse res = service.confirmarEfectivo(1L);
        assertThat(res.estado()).isEqualTo(EstadoRegistroBot.REGISTRADO);
        assertThat(res.tipoReceptor()).isEqualTo(TipoReceptorBot.EMPLEADO);
    }

    @Test
    void confirmarEfectivo_proveedor() {
        when(registroRepo.findById(1L)).thenReturn(Optional.of(efectivoPendiente("Dental", "3000")));
        when(configRepo.findAllByOrderByEmpleadoNombreAsc()).thenReturn(List.of());
        when(proveedorRepo.findByActivoTrue()).thenReturn(List.of(
                Proveedor.builder().id(3L).nombre("Dental SA").activo(true).build()));
        when(deudaProveedorRepo.findByProveedorIdOrderByFechaCreacionDesc(3L)).thenReturn(List.of());
        when(registroRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        RegistroPagoBotResponse res = service.confirmarEfectivo(1L);
        assertThat(res.tipoReceptor()).isEqualTo(TipoReceptorBot.PROVEEDOR);
    }

    @Test
    void confirmarEfectivo_noPendiente_business() {
        RegistroPagoBot reg = efectivoPendiente("Carlos", "3000");
        reg.setEstado(EstadoRegistroBot.REGISTRADO);
        when(registroRepo.findById(1L)).thenReturn(Optional.of(reg));
        assertThatThrownBy(() -> service.confirmarEfectivo(1L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void confirmarEfectivo_desconocido_rechaza() {
        when(registroRepo.findById(1L)).thenReturn(Optional.of(efectivoPendiente("Fulano", "3000")));
        when(configRepo.findAllByOrderByEmpleadoNombreAsc()).thenReturn(List.of());
        when(proveedorRepo.findByActivoTrue()).thenReturn(List.of());
        when(registroRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        RegistroPagoBotResponse res = service.confirmarEfectivo(1L);
        assertThat(res.estado()).isEqualTo(EstadoRegistroBot.RECHAZADO);
    }

    @Test
    void rechazarEfectivo_ok() {
        when(registroRepo.findById(1L)).thenReturn(Optional.of(efectivoPendiente("Carlos", "3000")));
        when(registroRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        RegistroPagoBotResponse res = service.rechazarEfectivo(1L, "no corresponde");
        assertThat(res.estado()).isEqualTo(EstadoRegistroBot.RECHAZADO);
    }

    @Test
    void rechazarEfectivo_noPendiente_business() {
        RegistroPagoBot reg = efectivoPendiente("Carlos", "3000");
        reg.setEstado(EstadoRegistroBot.REGISTRADO);
        when(registroRepo.findById(1L)).thenReturn(Optional.of(reg));
        assertThatThrownBy(() -> service.rechazarEfectivo(1L, "x")).isInstanceOf(BusinessException.class);
    }
}
