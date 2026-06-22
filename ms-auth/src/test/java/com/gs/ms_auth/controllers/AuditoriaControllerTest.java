package com.gs.ms_auth.controllers;

import com.gs.ms_auth.config.SecurityConfig;
import com.gs.ms_auth.model.AuditoriaEvento;
import com.gs.ms_auth.service.AuditoriaService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuditoriaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class AuditoriaControllerTest {

    @Autowired private MockMvc mvc;
    @MockBean private AuditoriaService auditoriaService;

    @Test
    void listar_200_mapeaEventos() throws Exception {
        when(auditoriaService.listarTodos()).thenReturn(List.of(
                new AuditoriaEvento("admin", "LOGIN", "Inicio de sesion", "Sesion", null)));

        mvc.perform(get("/api/auth/auditoria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuario").value("admin"))
                .andExpect(jsonPath("$[0].detalle").value(""));   // null → ""
    }
}
