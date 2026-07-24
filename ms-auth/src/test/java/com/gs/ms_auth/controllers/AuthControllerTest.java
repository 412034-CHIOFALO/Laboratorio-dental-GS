package com.gs.ms_auth.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gs.ms_auth.config.SecurityConfig;
import com.gs.ms_auth.model.Rol;
import com.gs.ms_auth.model.Usuario;
import com.gs.ms_auth.service.AuditoriaService;
import com.gs.ms_auth.service.UsuarioService;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ResourceServerAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private JwtEncoder jwtEncoder;
    @MockBean private AuthenticationManager authenticationManager;
    @MockBean private UsuarioService usuarioService;
    @MockBean private AuditoriaService auditoriaService;

    private Jwt fakeJwt() {
        return Jwt.withTokenValue("tok-123").header("alg", "none")
                .claim("sub", "admin").issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600)).build();
    }

    @Test
    void login_ok_devuelveAccessToken() throws Exception {
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("admin", "x",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        when(jwtEncoder.encode(any())).thenReturn(fakeJwt());
        when(usuarioService.buscarPorUsername("admin")).thenReturn(
                Usuario.builder().id(1L).username("admin").rol(Rol.ADMIN).terminosAceptados(true).build());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("tok-123"))
                .andExpect(jsonPath("$.terminosAceptados").value(true));
    }

    @Test
    void login_credencialesInvalidas_401() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_cuentaDeshabilitada_403() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("disabled"));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"pendiente\",\"password\":\"clave123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_ok_201() throws Exception {
        Usuario u = Usuario.builder().id(1L).username("jperez").rol(Rol.TECNICO).build();
        when(usuarioService.registrar(any())).thenReturn(u);

        String body = "{\"nombre\":\"Juan\",\"apellido\":\"Perez\",\"username\":\"jperez\","
                + "\"password\":\"password1\",\"rol\":\"TECNICO\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("jperez"));
    }

    @Test
    void register_usernameEnUso_409() throws Exception {
        when(usuarioService.registrar(any())).thenThrow(new IllegalArgumentException("ya en uso"));

        String body = "{\"nombre\":\"Juan\",\"apellido\":\"Perez\",\"username\":\"jperez\","
                + "\"password\":\"password1\",\"rol\":\"TECNICO\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void listarUsuarios_200() throws Exception {
        when(usuarioService.listarTodos()).thenReturn(List.of(
                Usuario.builder().id(1L).username("admin").rol(Rol.ADMIN).enabled(true).build()));
        mvc.perform(get("/api/auth/usuarios")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("admin"));
    }
}
