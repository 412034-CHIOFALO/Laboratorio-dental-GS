package com.gs.ms_catalogo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración central de la especificación OpenAPI 3 para ms-catálogo.
 * <p>
 * Expone la UI de Swagger en {@code /swagger-ui.html} y el JSON de la spec
 * en {@code /v3/api-docs}. Ambas rutas están permitidas sin autenticación
 * (ver {@link SecurityConfig}).
 * </p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Construye el bean {@link OpenAPI} con metadatos del servicio y el esquema
     * de seguridad Bearer JWT requerido por la mayoría de los endpoints.
     *
     * @return instancia configurada de {@link OpenAPI}
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MS-Catálogo API — Laboratorio G&S")
                .version("1.0.0")
                .description("Catálogo de trabajos dentales del laboratorio: tipos de prótesis, coronas, férulas y demás. "
                    + "Cada ítem define precio de referencia y receta de materiales de stock necesarios para su elaboración.")
                .contact(new Contact()
                    .name("Laboratorio G&S — TFI")
                    .email("nicolas.mauricio.chiofalo@gmail.com")))
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
            .components(new Components()
                .addSecuritySchemes("BearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Token JWT obtenido del endpoint POST /api/auth/login en ms-auth")));
    }
}
