package com.gs.ms_stock.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de la documentación OpenAPI 3 (Swagger UI) para el microservicio ms-stock.
 *
 * <p>Expone la UI en {@code /swagger-ui.html} y el contrato en {@code /v3/api-docs}.
 * Todos los endpoints están protegidos con JWT Bearer; el token se obtiene mediante
 * {@code POST /api/auth/login} en ms-auth.</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Define los metadatos generales de la API y el esquema de seguridad JWT.
     *
     * @return instancia {@link OpenAPI} configurada para Laboratorio G&amp;S
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MS-Stock API — Laboratorio G&S")
                .version("1.0.0")
                .description("Control de inventario de materiales del laboratorio dental. "
                    + "Gestiona el stock de insumos (acrílico, dientes, alambre, yeso, etc.), "
                    + "alertas de stock bajo y descuento automático de materiales al registrar "
                    + "un pedido según la receta del catálogo.")
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
