package com.gs.ms_pedidos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MS-Pedidos API — Laboratorio G&S")
                .version("1.0.0")
                .description("Gestión del ciclo de vida completo de los pedidos del laboratorio dental: creación, seguimiento de estados (RECIBIDO → EN_PROCESO → CONTROL → LISTO → ENTREGADO), asignación de técnicos y control de fechas de entrega.")
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
