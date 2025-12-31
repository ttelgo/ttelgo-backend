package com.tiktel.ttelgo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ttelgoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TTelGo Backend API")
                        .description("TTelGo eSIM Platform Backend API Documentation")
                        .version("v0.0.1")
                        .contact(new Contact()
                                .name("TikTel Ltd. UK")
                                .email("support@tiktel.com"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://tiktel.com")));
    }
}

