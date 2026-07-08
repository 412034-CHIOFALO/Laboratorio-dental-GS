package com.gs.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
 *   - POST /api/auth/login   → login
 *   - GET  /actuator/health  → health check del propio gateway
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
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                // Health checks del gateway para orquestadores y Eureka
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Nota: el JWK Set no se rutea por el gateway; cada MS (y el propio
                // gateway) lo fetchea directo de ms-auth vía JWT_JWK_URI.
                // Endpoints del bot de WhatsApp: pasan sin JWT, los protege la API key
                // (X-Bot-Api-Key) que valida ms-finanzas internamente.
                //   - pago-automatico → transferencia leída del comprobante
                //   - pago-efectivo   → borrador de efectivo (pendiente de confirmar)
                .requestMatchers(HttpMethod.POST, "/api/finanzas/sueldos/pago-automatico").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/finanzas/sueldos/pago-efectivo").permitAll()
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
        // Orígenes configurables vía ALLOWED_ORIGINS (separados por coma), igual que
        // en los 5 MS. Default = localhost:4200 para dev. En prod (nginx, mismo
        // origen) no se usa CORS, pero queda correcto si se accede al gateway directo.
        String origins = System.getenv("ALLOWED_ORIGINS");
        List<String> allowed = (origins == null || origins.isBlank())
            ? List.of("http://localhost:4200")
            : List.of(origins.split("\\s*,\\s*"));

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowed);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
