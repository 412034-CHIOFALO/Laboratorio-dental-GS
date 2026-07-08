package com.gs.ms_pedidos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CORS lo maneja unicamente el gateway (unico punto de entrada del browser).
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // Swagger / OpenAPI — acceso sin autenticación
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Subir escaneos/docs — TECNICO también (trabajan con los archivos)
                .requestMatchers(HttpMethod.POST, "/api/pedidos/*/escaneos/**")
                    .hasAnyRole("ADMIN", "ADMINISTRATIVO", "TECNICO")
                .requestMatchers(HttpMethod.DELETE, "/api/pedidos/*/escaneos/**")
                    .hasAnyRole("ADMIN", "ADMINISTRATIVO", "TECNICO")
                .requestMatchers(HttpMethod.POST, "/api/pedidos/*/docs/**")
                    .hasAnyRole("ADMIN", "ADMINISTRATIVO", "TECNICO", "ODONTOLOGO")
                // Lectura de pedidos — ADMIN, ADMINISTRATIVO, TECNICO
                .requestMatchers(HttpMethod.GET, "/api/pedidos/**")
                    .hasAnyRole("ADMIN", "ADMINISTRATIVO", "TECNICO")
                // Crear pedido — ADMIN, ADMINISTRATIVO, ODONTOLOGO
                .requestMatchers(HttpMethod.POST, "/api/pedidos/**")
                    .hasAnyRole("ADMIN", "ADMINISTRATIVO", "ODONTOLOGO")
                // Actualizar pedido completo — ADMIN, ADMINISTRATIVO
                .requestMatchers(HttpMethod.PUT, "/api/pedidos/**")
                    .hasAnyRole("ADMIN", "ADMINISTRATIVO")
                // Cambio de estado (kanban) — ADMIN, ADMINISTRATIVO, TECNICO
                .requestMatchers(HttpMethod.PATCH, "/api/pedidos/**")
                    .hasAnyRole("ADMIN", "ADMINISTRATIVO", "TECNICO")
                // Eliminación — solo ADMIN
                .requestMatchers(HttpMethod.DELETE, "/api/pedidos/**").hasRole("ADMIN")
                // ── Odontólogos (clientes del lab) ───────────────────────
                // Lectura/búsqueda — cualquier rol autenticado
                .requestMatchers(HttpMethod.GET, "/api/odontologos/**")
                    .hasAnyRole("ADMIN", "ADMINISTRATIVO", "TECNICO")
                // Alta/edición de odontólogos — ADMIN y ADMINISTRATIVO
                .requestMatchers(HttpMethod.POST, "/api/odontologos/**")
                    .hasAnyRole("ADMIN", "ADMINISTRATIVO")
                .requestMatchers(HttpMethod.PUT, "/api/odontologos/**")
                    .hasAnyRole("ADMIN", "ADMINISTRATIVO")
                // Desactivar odontólogo — solo ADMIN
                .requestMatchers(HttpMethod.DELETE, "/api/odontologos/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        return http.build();
    }

    /**
     * Extrae el claim "roles" del JWT emitido por ms-auth.
     * Ejemplo: "ROLE_ADMIN" → SimpleGrantedAuthority("ROLE_ADMIN")
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String roles = jwt.getClaimAsString("roles");
            if (roles == null || roles.isBlank()) return Collections.emptyList();
            return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r))
                .collect(Collectors.toList());
        });
        return converter;
    }
}
