package com.gs.ms_stock.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gs.ms_stock.config.SecurityConfig;
import com.gs.ms_stock.dto.MaterialRequest;
import com.gs.ms_stock.dto.MaterialResponse;
import com.gs.ms_stock.dto.MovimientoRequest;
import com.gs.ms_stock.exception.ResourceNotFoundException;
import com.gs.ms_stock.model.CategoriaMaterial;
import com.gs.ms_stock.model.TipoMovimiento;
import com.gs.ms_stock.service.IStockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StockController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class StockControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private IStockService service;

    private MaterialResponse dto() {
        return new MaterialResponse(1L, "Zirconio", "desc", CategoriaMaterial.CERAMICA,
                10.0, 2.0, "gr", new BigDecimal("100"), "Prov", true, false, true, null);
    }

    private MaterialRequest req() {
        MaterialRequest r = new MaterialRequest();
        r.setNombre("Zirconio"); r.setCategoria(CategoriaMaterial.CERAMICA);
        r.setStockActual(10.0); r.setStockMinimo(2.0); r.setUnidadMedida("gr");
        return r;
    }

    @Test
    void listar_200() throws Exception {
        when(service.listarActivos()).thenReturn(List.of(dto()));
        mvc.perform(get("/api/stock")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Zirconio"));
    }

    @Test
    void alertas_200() throws Exception {
        when(service.listarBajoStock()).thenReturn(List.of(dto()));
        mvc.perform(get("/api/stock/alertas")).andExpect(status().isOk());
        verify(service).listarBajoStock();
    }

    @Test
    void buscarPorId_200() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(dto());
        mvc.perform(get("/api/stock/1")).andExpect(status().isOk());
    }

    @Test
    void buscarPorId_404() throws Exception {
        when(service.buscarPorId(9L)).thenThrow(new ResourceNotFoundException("Material", 9L));
        mvc.perform(get("/api/stock/9")).andExpect(status().isNotFound());
    }

    @Test
    void crear_201() throws Exception {
        when(service.crear(any())).thenReturn(dto());
        mvc.perform(post("/api/stock").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req())))
                .andExpect(status().isCreated());
    }

    @Test
    void actualizar_200() throws Exception {
        when(service.actualizar(any(), any())).thenReturn(dto());
        mvc.perform(put("/api/stock/1").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req())))
                .andExpect(status().isOk());
    }

    @Test
    void registrarMovimiento_200() throws Exception {
        when(service.registrarMovimiento(any())).thenReturn(dto());
        MovimientoRequest m = new MovimientoRequest();
        m.setMaterialId(1L); m.setTipo(TipoMovimiento.ENTRADA); m.setCantidad(5.0);
        mvc.perform(post("/api/stock/movimiento").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(m)))
                .andExpect(status().isOk());
    }

    @Test
    void eliminar_204() throws Exception {
        mvc.perform(delete("/api/stock/1")).andExpect(status().isNoContent());
        verify(service).eliminar(1L);
    }
}
