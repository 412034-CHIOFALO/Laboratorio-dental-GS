package com.gys.ms_finanzas.config;

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
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final BotApiKeyFilter botApiKeyFilter;

    public SecurityConfig(BotApiKeyFilter botApiKeyFilter) {
        this.botApiKeyFilter = botApiKeyFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // El bot se autentica por API key (header X-Bot-Api-Key) antes del JWT
            .addFilterBefore(botApiKeyFilter, BearerTokenAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Endpoint del bot: lo autentica el BotApiKeyFilter (API key), no JWT
                .requestMatchers(HttpMethod.POST, "/api/finanzas/sueldos/pago-automatico").hasRole("ADMIN")
                // Cajas y cobros — solo ADMIN
                .requestMatchers("/api/finanzas/cajas/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/finanzas/cobros").hasRole("ADMIN")
                // Sueldos — solo ADMIN
                .requestMatchers("/api/finanzas/sueldos/**").hasRole("ADMIN")
                // Proveedores — ADMIN y ADMINISTRATIVO
                .requestMatchers("/api/finanzas/proveedores/**").hasAnyRole("ADMIN", "ADMINISTRATIVO")
                // Consulta de comprobantes y saldos — ADMIN y ADMINISTRATIVO
                .requestMatchers(HttpMethod.GET, "/api/finanzas/**")
                    .hasAnyRole("ADMIN", "ADMINISTRATIVO")
                // Emitir comprobante — ADMIN y ADMINISTRATIVO
                .requestMatchers(HttpMethod.POST, "/api/finanzas/comprobantes")
                    .hasAnyRole("ADMIN", "ADMINISTRATIVO")
                // Registrar cobro legacy — solo ADMIN (operación financiera definitiva)
                .requestMatchers(HttpMethod.PATCH, "/api/finanzas/comprobantes/*/cobrar")
                    .hasRole("ADMIN")
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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
