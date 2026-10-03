package com.bosch.ecommerce.price.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI priceServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Price Service API")
                .version("v1")
                .description("Owns product prices, price versions and price history. "
                        + "Prices in the seeded database are synthetic demo prices, not official Bosch prices. "
                        + "This service does NOT calculate tax."));
    }
}
