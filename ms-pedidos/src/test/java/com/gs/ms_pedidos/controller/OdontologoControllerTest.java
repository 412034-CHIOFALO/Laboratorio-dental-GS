package com.gs.ms_pedidos.controller;

import com.gs.ms_pedidos.config.SecurityConfig;
import com.gs.ms_pedidos.exception.ResourceNotFoundException;
import com.gs.ms_pedidos.service.IOdontologoService;
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

@WebMvcTest(controllers = OdontologoController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class OdontologoControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private IOdontologoService service;

    @Test
    void listar_sinQuery_usaListarActivos() throws Exception {
        when(service.listarActivos()).thenReturn(List.of());
        mvc.perform(get("/api/odontologos")).andExpect(status().isOk());
        verify(service).listarActivos();
    }

    @Test
    void listar_conQuery_buscaPorNombre() throws Exception {
        when(service.buscarPorNombre("gar")).thenReturn(List.of());
        mvc.perform(get("/api/odontologos").param("q", "gar")).andExpect(status().isOk());
        verify(service).buscarPorNombre("gar");
    }

    @Test
    void buscarPorId_200() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(null);
        mvc.perform(get("/api/odontologos/1")).andExpect(status().isOk());
    }

    @Test
    void buscarPorId_404() throws Exception {
        when(service.buscarPorId(9L)).thenThrow(new ResourceNotFoundException("Odontologo", 9L));
        mvc.perform(get("/api/odontologos/9")).andExpect(status().isNotFound());
    }

    @Test
    void desactivar_204() throws Exception {
        mvc.perform(delete("/api/odontologos/1")).andExpect(status().isNoContent());
        verify(service).desactivar(1L);
    }
}
