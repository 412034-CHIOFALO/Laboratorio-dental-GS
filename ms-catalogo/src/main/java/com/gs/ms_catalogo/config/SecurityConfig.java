package com.gs.ms_catalogo.config;

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
                // Actuator — libre para health checks del orquestador
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                // Swagger / OpenAPI — sin autenticación para desarrollo y revisión
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Lectura del catálogo — cualquier usuario autenticado
                .requestMatchers(HttpMethod.GET, "/api/catalogo", "/api/catalogo/**").authenticated()
                // Modificación del catálogo — solo ADMIN
                .requestMatchers(HttpMethod.POST,   "/api/catalogo/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,    "/api/catalogo/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/catalogo/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        return http.build();
    }

    /**
     * Extrae el claim "roles" del JWT emitido por ms-auth.
     * El claim llega como String CSV: "ROLE_ADMIN" o "ROLE_TECNICO".
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
