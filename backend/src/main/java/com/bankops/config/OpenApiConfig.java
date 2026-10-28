package com.bankops.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI bankOpsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("BankOps - Banking Operations & Exception Management Platform API")
                        .description("Enterprise REST API for synthetic banking transaction monitoring, exception management workflow, operational risk control, and audit trail.")
                        .version("v1.0.0")
                        .contact(new Contact().name("Banking Technology Operations").email("ops-tech@bankops.internal"))
                        .license(new License().name("Internal Enterprise License").url("https://bankops.internal/license")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
