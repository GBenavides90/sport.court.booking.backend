package com.sport.court.booking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sport Court Booking API")
                        .version("1.0.0")
                        .description("API REST para gestión de canchas deportivas — reservas, listado y administración")
                        .contact(new Contact()
                                .name("Sport Court Booking")
                                .url("https://sport-court-booking-frotend.vercel.app")));
    }
}
