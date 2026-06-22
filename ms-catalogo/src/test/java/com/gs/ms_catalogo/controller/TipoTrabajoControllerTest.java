package com.gs.ms_catalogo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gs.ms_catalogo.config.SecurityConfig;
import com.gs.ms_catalogo.dto.TipoTrabajoRequest;
import com.gs.ms_catalogo.dto.TipoTrabajoResponse;
import com.gs.ms_catalogo.exception.ResourceNotFoundException;
import com.gs.ms_catalogo.model.Categoria;
import com.gs.ms_catalogo.service.ITipoTrabajoService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests del controller del catalogo. Se excluye la cadena de seguridad
 * (addFilters=false) para enfocar el test en el ruteo y el mapeo HTTP.
 */
@WebMvcTest(controllers = TipoTrabajoController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class TipoTrabajoControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ITipoTrabajoService service;

    private TipoTrabajoResponse dto() {
        return new TipoTrabajoResponse(1L, "Corona", "desc", new BigDecimal("15000"),
                Categoria.FIJA, 7, null, true, List.of(), null, null);
    }

    private TipoTrabajoRequest request() {
        TipoTrabajoRequest req = new TipoTrabajoRequest();
        req.setNombre("Corona");
        req.setPrecio(new BigDecimal("15000"));
        req.setCategoria(Categoria.FIJA);
        return req;
    }

    @Test
    void listar_sinParams_usaListarActivos() throws Exception {
        when(service.listarActivos()).thenReturn(List.of(dto()));

        mvc.perform(get("/api/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Corona"));
        verify(service).listarActivos();
    }

    @Test
    void listar_conNombre_buscaPorNombre() throws Exception {
        when(service.buscarPorNombre("cor")).thenReturn(List.of(dto()));

        mvc.perform(get("/api/catalogo").param("nombre", "cor"))
                .andExpect(status().isOk());
        verify(service).buscarPorNombre("cor");
        verify(service, never()).listarActivos();
    }

    @Test
    void listar_conCategoria_filtra() throws Exception {
        when(service.listarPorCategoria(Categoria.FIJA)).thenReturn(List.of(dto()));

        mvc.perform(get("/api/catalogo").param("categoria", "FIJA"))
                .andExpect(status().isOk());
        verify(service).listarPorCategoria(Categoria.FIJA);
    }

    @Test
    void buscarPorId_existente_200() throws Exception {
        when(service.buscarPorId(1L)).thenReturn(dto());

        mvc.perform(get("/api/catalogo/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void buscarPorId_inexistente_404() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new ResourceNotFoundException("TipoTrabajo", 99L));

        mvc.perform(get("/api/catalogo/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void crear_devuelve201() throws Exception {
        when(service.crear(any())).thenReturn(dto());

        mvc.perform(post("/api/catalogo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Corona"));
    }

    @Test
    void actualizar_devuelve200() throws Exception {
        when(service.actualizar(eq(1L), any())).thenReturn(dto());

        mvc.perform(put("/api/catalogo/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk());
    }

    @Test
    void eliminar_devuelve204() throws Exception {
        mvc.perform(delete("/api/catalogo/1"))
                .andExpect(status().isNoContent());
        verify(service).eliminar(1L);
    }
}
