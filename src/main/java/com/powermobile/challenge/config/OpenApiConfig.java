package com.powermobile.challenge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CRM & Contract Signing API")
                        .description("API for client management, proposal lifecycle, contract generation, and sequential signing flow")
                        .version("v1")
                        .contact(new Contact()
                                .name("Livia Almeida")
                                .email("liviamoraes49@gmail.com")));
    }
}

