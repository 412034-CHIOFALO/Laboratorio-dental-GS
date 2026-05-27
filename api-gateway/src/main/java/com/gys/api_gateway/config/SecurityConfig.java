package com.gys.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Primera línea de defensa: el Gateway rechaza requests sin JWT válido
 * ANTES de que lleguen a cualquier microservicio.
 *
 * Flujo:
 *   1. Angular envía Authorization: Bearer <token>
 *   2. Gateway valida la firma del JWT contra el JWK Set de ms-auth
 *   3. Si el JWT es válido, el request se enruta al MS correspondiente
 *      (con el header Authorization intacto)
 *   4. El MS destino valida el JWT de nuevo (defensa en profundidad)
 *      y aplica control de acceso por rol
 *
 * Rutas públicas (sin token):
 *   - POST /ms-auth/api/auth/login  → login
 *   - GET  /actuator/health          → health check del propio gateway
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Login — único endpoint realmente público (necesita llegar sin token)
                .requestMatchers("/ms-auth/api/auth/login").permitAll()
                // Health checks del gateway para orquestadores y Eureka
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // TODO: El endpoint /ms-auth/oauth2/jwks debe ser accesible para otros MS
                //       que fetchen el JWK Set en sus arranques. Habilitarlo aquí.
                .requestMatchers("/ms-auth/oauth2/jwks").permitAll()
                // Todo lo demás (todos los MS) requiere JWT válido firmado por ms-auth
                .anyRequest().authenticated()
            )
            // Valida JWT contra ms-auth usando la propiedad jwk-set-uri del application.properties
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    /**
     * CORS centralizado en el gateway.
     * Los microservicios individuales también tienen su propio CORS como segunda capa,
     * pero el gateway es el único punto de entrada real desde Angular.
     */
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
