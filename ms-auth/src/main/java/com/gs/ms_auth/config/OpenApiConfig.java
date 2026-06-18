package com.gs.ms_auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración global de la especificación OpenAPI 3.0 para ms-auth.
 * <p>
 * Expone la documentación interactiva en {@code /swagger-ui.html} y el
 * contrato JSON en {@code /v3/api-docs}.
 * </p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Define los metadatos globales de la API y el esquema de seguridad
     * BearerAuth (JWT RS256) que aplica a todos los endpoints protegidos.
     *
     * @return instancia de {@link OpenAPI} con la configuración del laboratorio G&amp;S
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MS-Auth API — Laboratorio G&S")
                .version("1.0.0")
                .description("Autenticación JWT (RS256), registro y aprobación de usuarios, gestión de estados y bitácora de auditoría inmutable. Emite tokens para todos los microservicios del sistema.")
                .contact(new Contact()
                    .name("Laboratorio G&S — TFI")
                    .email("nicolas.mauricio.chiofalo@gmail.com")))
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
            .components(new Components()
                .addSecuritySchemes("BearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Token JWT obtenido del endpoint POST /api/auth/login")));
    }
}
