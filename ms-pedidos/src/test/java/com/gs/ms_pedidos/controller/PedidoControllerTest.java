package com.gs.ms_pedidos.controller;

import com.gs.ms_pedidos.config.SecurityConfig;
import com.gs.ms_pedidos.exception.ResourceNotFoundException;
import com.gs.ms_pedidos.model.EstadoPedido;
import com.gs.ms_pedidos.service.IPedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PedidoController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class PedidoControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private IPedidoService service;

    @Test
    void listarTodos_200() throws Exception {
        when(service.listarTodos()).thenReturn(List.of());
        mvc.perform(get("/api/pedidos")).andExpect(status().isOk());
    }

    @Test
    void listarActivos_200() throws Exception {
        when(service.listarActivos()).thenReturn(List.of());
        mvc.perform(get("/api/pedidos/activos")).andExpect(status().isOk());
    }

    @Test
    void listarAtrasados_200() throws Exception {
        when(service.listarAtrasados()).thenReturn(List.of());
        mvc.perform(get("/api/pedidos/atrasados")).andExpect(status().isOk());
    }

    @Test
    void listarPorEstado_200() throws Exception {
        when(service.listarPorEstado(EstadoPedido.LISTO)).thenReturn(List.of());
        mvc.perform(get("/api/pedidos/estado/LISTO")).andExpect(status().isOk());
    }

    @Test
    void buscarPorId_200() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(null);
        mvc.perform(get("/api/pedidos/1")).andExpect(status().isOk());
    }

    @Test
    void buscarPorId_404() throws Exception {
        when(service.buscarPorId(9L)).thenThrow(new ResourceNotFoundException("Pedido", 9L));
        mvc.perform(get("/api/pedidos/9")).andExpect(status().isNotFound());
    }

    @Test
    void actualizarEstado_200() throws Exception {
        when(service.actualizarEstado(1L, EstadoPedido.EN_PROCESO)).thenReturn(null);
        mvc.perform(patch("/api/pedidos/1/estado").param("nuevoEstado", "EN_PROCESO"))
                .andExpect(status().isOk());
    }

    @Test
    void eliminar_204() throws Exception {
        mvc.perform(delete("/api/pedidos/1")).andExpect(status().isNoContent());
        verify(service).eliminar(1L);
    }
}
