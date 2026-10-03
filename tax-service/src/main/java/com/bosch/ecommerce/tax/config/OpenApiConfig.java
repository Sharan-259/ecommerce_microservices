package com.bosch.ecommerce.tax.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taxServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Tax Service API")
                .version("v1")
                .description("Owns HSN classification, GST rules and tax calculation. "
                        + "Seed data is a DEMONSTRATION dataset (HSN headings/candidates) and must not be used "
                        + "for real invoicing without verification. This service does NOT own prices."));
    }
}
