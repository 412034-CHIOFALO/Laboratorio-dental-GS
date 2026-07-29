package com.gs.ms_pedidos.service;

import com.gs.ms_pedidos.dto.EntregaRequest;
import com.gs.ms_pedidos.dto.OdontologoResponse;
import com.gs.ms_pedidos.dto.PedidoRequest;
import com.gs.ms_pedidos.exception.BusinessException;
import com.gs.ms_pedidos.exception.ResourceNotFoundException;
import com.gs.ms_pedidos.model.EstadoPedido;
import com.gs.ms_pedidos.model.Odontologo;
import com.gs.ms_pedidos.model.Pedido;
import com.gs.ms_pedidos.repository.OdontologoRepository;
import com.gs.ms_pedidos.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private OdontologoRepository odontologoRepository;
    @Mock private IOdontologoService odontologoService;
    @Mock private ConsumoStockService consumoStockService;
    @Mock private NotificacionBotService notificacionBotService;
    @Mock private EmisionComprobanteService emisionComprobanteService;
    @Mock private AuditoriaClient auditoria;
    @InjectMocks private PedidoService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "diasLimiteAtraso", 6);
    }

    private Pedido pedido(EstadoPedido estado) {
        return Pedido.builder()
                .id(1L).nroPedido("PED-1").estado(estado)
                .odontologoId(7L).odontologoNombre("Garcia").trabajo("Corona")
                .build();
    }

    private PedidoRequest reqConId() {
        PedidoRequest r = new PedidoRequest();
        r.setOdontologoId(7L);
        r.setPaciente("Juan");
        r.setTrabajo("Corona");
        return r;
    }

    @Test
    void listarTodos_mapea() {
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido(EstadoPedido.RECIBIDO)));
        assertThat(service.listarTodos()).hasSize(1);
    }

    @Test
    void listarActivos_excluyeListo() {
        when(pedidoRepository.findByEstadoNot(EstadoPedido.LISTO)).thenReturn(List.of(pedido(EstadoPedido.EN_PROCESO)));
        assertThat(service.listarActivos()).hasSize(1);
    }

    @Test
    void listarPorEstado_filtra() {
        when(pedidoRepository.findByEstado(EstadoPedido.LISTO)).thenReturn(List.of(pedido(EstadoPedido.LISTO)));
        assertThat(service.listarPorEstado(EstadoPedido.LISTO)).hasSize(1);
    }

    @Test
    void buscarPorId_404() {
        when(pedidoRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscarPorId(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarAtrasados_devuelveLista() {
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido(EstadoPedido.EN_PROCESO)));
        assertThat(service.listarAtrasados()).isNotNull();
    }

    @Test
    void crear_conOdontologoId_usaExistente() {
        when(odontologoService.buscarPorId(7L)).thenReturn(new OdontologoResponse(
                7L, "Garcia", null, null, null, null, null, null, null, true, null, null, null, false));
        when(pedidoRepository.count()).thenReturn(0L);
        when(pedidoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var res = service.crear(reqConId());
        assertThat(res).isNotNull();
        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    void crear_sinId_findOrCreatePorNombre() {
        PedidoRequest r = new PedidoRequest();
        r.setOdontologoNombre("Nuevo Dentista");
        r.setTrabajo("Placa");
        when(odontologoService.buscarOCrearPorNombre("Nuevo Dentista"))
                .thenReturn(Odontologo.builder().id(9L).nombre("Nuevo Dentista").build());
        when(pedidoRepository.count()).thenReturn(3L);
        when(pedidoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.crear(r);
        verify(odontologoService).buscarOCrearPorNombre("Nuevo Dentista");
    }

    @Test
    void actualizarEstado_aEnProceso_disparaConsumoStock() {
        Pedido p = pedido(EstadoPedido.RECIBIDO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(pedidoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.actualizarEstado(1L, EstadoPedido.EN_PROCESO);

        verify(consumoStockService).descontarSiCorresponde(p);
    }

    @Test
    void actualizarEstado_aListo_notificaOdontologo() {
        Pedido p = pedido(EstadoPedido.CONTROL);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(pedidoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(odontologoRepository.findById(7L)).thenReturn(Optional.of(Odontologo.builder().id(7L).nombre("Garcia").build()));

        service.actualizarEstado(1L, EstadoPedido.LISTO);

        verify(notificacionBotService).notificarPedidoListo(anyString(), anyString(), any(Odontologo.class));
    }

    @Test
    void actualizarEstado_404() {
        when(pedidoRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.actualizarEstado(9L, EstadoPedido.LISTO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizar_404() {
        when(pedidoRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.actualizar(9L, reqConId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void marcarEntregado_noListo_lanzaBusiness() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido(EstadoPedido.EN_PROCESO)));

        EntregaRequest e = new EntregaRequest();
        e.setRetiradoPor("Cadete");
        assertThatThrownBy(() -> service.marcarEntregado(1L, e))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void marcarEntregado_listo_ok() {
        Pedido p = pedido(EstadoPedido.LISTO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(p));
        when(pedidoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        EntregaRequest e = new EntregaRequest();
        e.setRetiradoPor("  Cadete  ");
        service.marcarEntregado(1L, e);

        assertThat(p.getEstado()).isEqualTo(EstadoPedido.ENTREGADO);
        assertThat(p.getRetiradoPor()).isEqualTo("Cadete");
    }

    @Test
    void eliminar_inexistente_404() {
        when(pedidoRepository.existsById(9L)).thenReturn(false);
        assertThatThrownBy(() -> service.eliminar(9L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eliminar_ok() {
        when(pedidoRepository.existsById(1L)).thenReturn(true);
        service.eliminar(1L);
        verify(pedidoRepository).deleteById(1L);
    }
}
