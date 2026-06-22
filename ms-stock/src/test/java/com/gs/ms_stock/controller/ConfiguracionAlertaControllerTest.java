package com.gs.ms_stock.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gs.ms_stock.config.SecurityConfig;
import com.gs.ms_stock.dto.ConfiguracionAlertaRequest;
import com.gs.ms_stock.model.ConfiguracionAlerta;
import com.gs.ms_stock.repository.ConfiguracionAlertaRepository;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ConfiguracionAlertaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class ConfiguracionAlertaControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private ConfiguracionAlertaRepository configRepo;

    @Test
    void obtener_existente_devuelveConfig() throws Exception {
        ConfiguracionAlerta c = ConfiguracionAlerta.builder()
                .id(1L).alertasActivas(true).adminWhatsappPhone("549110").build();
        when(configRepo.findById(1L)).thenReturn(Optional.of(c));

        mvc.perform(get("/api/stock/configuracion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertasActivas").value(true));
    }

    @Test
    void obtener_inexistente_creaConfigPorDefecto() throws Exception {
        when(configRepo.findById(1L)).thenReturn(Optional.empty());
        when(configRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        mvc.perform(get("/api/stock/configuracion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertasActivas").value(false));
        verify(configRepo).save(any());
    }

    @Test
    void actualizar_guardaCambios() throws Exception {
        ConfiguracionAlerta c = ConfiguracionAlerta.builder().id(1L).build();
        when(configRepo.findById(1L)).thenReturn(Optional.of(c));
        when(configRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        ConfiguracionAlertaRequest req = new ConfiguracionAlertaRequest();
        req.setAdminWhatsappPhone("5491199999");
        req.setAlertasActivas(true);

        mvc.perform(put("/api/stock/configuracion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminWhatsappPhone").value("5491199999"));
    }
}
