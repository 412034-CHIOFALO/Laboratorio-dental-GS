package com.gs.ms_pedidos.service;

import com.gs.ms_pedidos.client.FinanzasClient;
import com.gs.ms_pedidos.model.Pedido;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmisionComprobanteServiceTest {

    @Mock private FinanzasClient finanzasClient;
    @InjectMocks private EmisionComprobanteService service;

    private Pedido pedido() {
        return Pedido.builder()
                .id(1L).nroPedido("GS-2026-0001")
                .odontologoId(7L).odontologoNombre("Dr. García")
                .trabajo("Corona").build();
    }

    @Test
    void conMonto_generaComprobante_yMarcaFlag() {
        Pedido p = pedido();
        service.emitirSiCorresponde(p, new BigDecimal("15000"));
        assertThat(p.isComprobanteGenerado()).isTrue();
        verify(finanzasClient).emitirComprobante(any());
    }

    @Test
    void sinMonto_noGenera() {
        Pedido p = pedido();
        service.emitirSiCorresponde(p, null);
        assertThat(p.isComprobanteGenerado()).isFalse();
        verify(finanzasClient, never()).emitirComprobante(any());
    }

    @Test
    void montoCeroONegativo_noGenera() {
        Pedido p = pedido();
        service.emitirSiCorresponde(p, BigDecimal.ZERO);
        assertThat(p.isComprobanteGenerado()).isFalse();
        verify(finanzasClient, never()).emitirComprobante(any());
    }

    @Test
    void yaGenerado_noVuelveALlamar() {
        Pedido p = pedido();
        p.setComprobanteGenerado(true);
        service.emitirSiCorresponde(p, new BigDecimal("15000"));
        verify(finanzasClient, never()).emitirComprobante(any());
    }

    @Test
    void finanzasFalla_noRompe_flagQuedaFalse() {
        Pedido p = pedido();
        when(finanzasClient.emitirComprobante(any())).thenThrow(new RuntimeException("ms-finanzas caído"));
        service.emitirSiCorresponde(p, new BigDecimal("15000"));   // no lanza
        assertThat(p.isComprobanteGenerado()).isFalse();           // reintentable
    }
}
