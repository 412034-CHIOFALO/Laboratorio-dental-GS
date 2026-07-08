package com.gs.ms_pedidos.service;

import com.gs.ms_pedidos.client.CatalogoClient;
import com.gs.ms_pedidos.client.StockClient;
import com.gs.ms_pedidos.client.dto.IngredienteRecetaDTO;
import com.gs.ms_pedidos.client.dto.TipoTrabajoDTO;
import com.gs.ms_pedidos.model.Pedido;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests del descuento automatico de stock (best-effort): idempotencia, trabajo
 * custom sin receta, fallos de catalogo/stock y descuento parcial.
 */
@ExtendWith(MockitoExtension.class)
class ConsumoStockServiceTest {

    @Mock private CatalogoClient catalogoClient;
    @Mock private StockClient stockClient;
    @InjectMocks private ConsumoStockService service;

    private Pedido pedido(boolean consumido, Long catalogoId) {
        return Pedido.builder()
                .id(1L).nroPedido("P-001")
                .stockConsumido(consumido)
                .catalogoTrabajoId(catalogoId)
                .build();
    }

    private IngredienteRecetaDTO ing(Long matId, String cantidad) {
        return new IngredienteRecetaDTO(matId, "Zirconio", new BigDecimal(cantidad), "gr");
    }

    @Test
    void yaConsumido_skipIdempotente() {
        boolean r = service.descontarSiCorresponde(pedido(true, 5L));
        assertThat(r).isTrue();
        verifyNoInteractions(catalogoClient, stockClient);
    }

    @Test
    void sinCatalogoTrabajoId_trabajoCustom_skip() {
        boolean r = service.descontarSiCorresponde(pedido(false, null));
        assertThat(r).isTrue();
        verifyNoInteractions(catalogoClient);
    }

    @Test
    void catalogoFalla_devuelveFalseSinDescontar() {
        when(catalogoClient.buscarPorId(5L)).thenThrow(new RuntimeException("ms-catalogo down"));

        boolean r = service.descontarSiCorresponde(pedido(false, 5L));

        assertThat(r).isFalse();
        verifyNoInteractions(stockClient);
    }

    @Test
    void recetaVacia_marcaConsumidoSinDescontar() {
        when(catalogoClient.buscarPorId(5L)).thenReturn(new TipoTrabajoDTO(5L, "Corona", List.of()));

        Pedido p = pedido(false, 5L);
        boolean r = service.descontarSiCorresponde(p);

        assertThat(r).isTrue();
        assertThat(p.isStockConsumido()).isTrue();
        verifyNoInteractions(stockClient);
    }

    @Test
    void happyPath_descuentaTodosLosIngredientes() {
        when(catalogoClient.buscarPorId(5L)).thenReturn(
                new TipoTrabajoDTO(5L, "Corona", List.of(ing(10L, "2"), ing(11L, "1"))));

        Pedido p = pedido(false, 5L);
        boolean r = service.descontarSiCorresponde(p);

        assertThat(r).isTrue();
        assertThat(p.isStockConsumido()).isTrue();
        verify(stockClient, times(2)).registrarMovimiento(any());
    }

    @Test
    void unMaterialFalla_marcaConsumidoPeroDevuelveFalse() {
        when(catalogoClient.buscarPorId(5L)).thenReturn(
                new TipoTrabajoDTO(5L, "Corona", List.of(ing(10L, "2"), ing(11L, "1"))));
        when(stockClient.registrarMovimiento(any()))
                .thenReturn(new Object())
                .thenThrow(new RuntimeException("stock down"));

        Pedido p = pedido(false, 5L);
        boolean r = service.descontarSiCorresponde(p);

        assertThat(r).isFalse();          // hubo 1 error
        assertThat(p.isStockConsumido()).isTrue(); // pero al menos 1 OK
    }
}
