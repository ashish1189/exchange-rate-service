package com.crewmeister.cmcodingchallenge.config;

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
                        .title("Exchange Rate Service")
                        .description("""
                                EUR foreign exchange rate service powered by Deutsche Bundesbank data.
                                Provides daily EUR-FX rates for 30 currencies sourced from the Bundesbank SDMX REST API.
                                Rates are refreshed daily at 18:00 on business days.
                                When no rate exists for a requested date (weekend or public holiday),
                                the most recent prior business day rate is returned automatically.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Ashish Deshpande")
                                .email("ashishdeshpande123@gmail.com")));
    }
}
