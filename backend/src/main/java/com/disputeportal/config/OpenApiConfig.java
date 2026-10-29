package com.disputeportal.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API description for integrators. Served only when springdoc is enabled (dev and demo profiles).
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI disputePortalOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Transaction Disputes API")
                        .version("0.1.0")
                        .description("Customers view transactions and raise disputes. Operations reviewers "
                                + "under /api/review decide them. All endpoints except health need a bearer token."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
